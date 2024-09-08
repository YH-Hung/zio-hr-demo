package org.shrimp

import zio.*

import scala.io.StdIn

object ZIORecap extends ZIOAppDefault{
  // ZIO = data structure describing arbitrary computations (include side effects)
  // effects = computations as value

  val meaningOfLift: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong")

  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLift)

  val improvedMOL = meaningOfLift.map(_ * 2)
  val printingMOL = meaningOfLift.flatMap(mol => ZIO.succeed(println(mol)))

  val smallProgram = for {
    _ <- Console.printLine("what's your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _ <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()

  // Error handling
  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("Trying something")
    val string : String = null
    string.length
  }

  val catchError: ZIO[Any, Nothing, Any] = anAttempt.catchAll(e => ZIO.succeed("Returning some different value"))
  val catchSelective: ZIO[Any, Throwable, Any] = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed("Ignoring runtime exception")
  }

  // fibers
  // *> : zip two effects and only retain right
  // In scala, method with singular argument can be used as an infix operator
  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield a -> b  // take 2s

  val aPairPar = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a <- fibA.join
    b <- fibB.join
  } yield a -> b  // take 1s

  val interruptedFiber = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted!"))).fork
    _  <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val ignoredInterruption = for {
    fib <- ZIO.uninterruptible(delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted!")))).fork
    _  <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val aPairPar_v2 = delayedValue.zipPar(delayedValue)
  val randomX10 = ZIO.collectAllPar((1 to 10).map(_ => delayedValue)) // similar with traversePar

  // dependencies
  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"subscribed $user")
    } yield ()
  }

  object UserSubscription {
    val live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction((emailS, userD) => new UserSubscription(emailS, userD))
  }

  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }
  
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService)
  }

  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"inserted $user")
  }
  
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(new UserDatabase(_))
  }
  
  class ConnectionPool(nConnection: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  
  object ConnectionPool {
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] = ZLayer.succeed(ConnectionPool(nConnections))
  }

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _ <- sub.subscribeUser(user)
  } yield ()

  val program = for {
    _ <- subscribe(User("Wan", "wan@rock.com"))
    _ <- subscribe(User("Ming", "ming@rock.com"))
  } yield ()

  case class Connection()

  override def run = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )
}
