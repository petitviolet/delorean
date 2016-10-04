//: ----------------------------------------------------------------------------
//: Copyright (C) 2016 Verizon.  All Rights Reserved.
//:
//:   Licensed under the Apache License, Version 2.0 (the "License");
//:   you may not use this file except in compliance with the License.
//:   You may obtain a copy of the License at
//:
//:       http://www.apache.org/licenses/LICENSE-2.0
//:
//:   Unless required by applicable law or agreed to in writing, software
//:   distributed under the License is distributed on an "AS IS" BASIS,
//:   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//:   See the License for the specific language governing permissions and
//:   limitations under the License.
//:
//: ----------------------------------------------------------------------------
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

import scalaz.{-\/, \/-}
import scalaz.concurrent.{Strategy, Task}

package object delorean {

  implicit class FutureAPI[A](self: => Future[A]) {

    def toTask(implicit ec: ExecutionContext, S: Strategy): Task[A] = {
      Task async { cb =>
        self onComplete {
          case Success(a) => S { cb(\/-(a)) }
          case Failure(t) => S { cb(-\/(t)) }
        }
      }
    }
  }

  implicit class TaskAPI[A](val self: Task[A]) extends AnyVal {

    def unsafeToFuture(): Future[A] = {
      val p = Promise[A]()

      self runAsync {
        case \/-(a) => p.complete(Success(a)); ()
        case -\/(t) => p.complete(Failure(t)); ()
      }

      p.future
    }
  }
}