import java.util.Scanner

import Test.MatrixMultiplicationActor.Calculation
import Test.Multiplier.{Multiply, Res}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object Test extends App {
  val scanner = new Scanner(System.in)
  println("enter the n*n matrix number you want: ")
  val system = ActorSystem("matrix")

  val multiplyActor = system.actorOf(Props[Multiplier])
  val matrixActor = system.actorOf(
    MatrixMultiplicationActor.props(multiplyActor),
    "matrixSystem"
  )

  var n: Int = scanner.nextInt()
  val a: Array[Array[Int]] =
    Array.fill(n, n)(Random.nextInt(50))
  val b: Array[Array[Int]] = Array.fill(n, n)(Random.nextInt(50))
  val c: Array[Array[Int]] = Array.fill(n, n)(0)
  var res = 0

  class MatrixMultiplicationActor(multiplier: ActorRef) extends Actor {
    implicit val timeout = Timeout(10 seconds)

    override def receive: Receive = {
      case Calculation(c, a, b, i, j, k) => {
        multiplier ! Multiply(a(i)(k), b(k)(j), i, j, c)
      }
      case Res(x, i, j, c) => {
        c(i)(j) += x
      }
    }
  }

  class Multiplier extends Actor {
    override def receive: Receive = {
      //Can't mutate data using multiple actor
      //It's not thread safe
      //Ask is a blocking call
      case Multiply(a, b, i, j, c) => {
        sender() ! Res(a * b, i, j, c)
      }

    }

  }

  implicit val timeout = Timeout(10 seconds)

  object MatrixMultiplicationActor {
    def props(multiplier: ActorRef) =
      Props(new MatrixMultiplicationActor(multiplier))

    case class Calculation(c: Array[Array[Int]],
                           a: Array[Array[Int]],
                           b: Array[Array[Int]],
                           i: Int,
                           j: Int,
                           k: Int)

  }

  object Multiplier {

    case class Multiply(a: Int, b: Int, i: Int, j: Int, c: Array[Array[Int]])
    case class Res(res: Int, i: Int, j: Int, c: Array[Array[Int]])

  }
  val t2 = System.currentTimeMillis()
  for (i <- a.indices) {
    for (j <- b.indices) {
      for (k <- c.indices) {
        matrixActor ! (Calculation(c, a, b, i, j, k))
      }
    }
  }
  println("using actors: " + (System.currentTimeMillis() - t2))
  Thread.sleep(5000)
  system.terminate()
}
