package como.tudux.bank.actors

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout
import como.tudux.bank.actors

//Cassandra Queries:
//select * from akka.messages;

import java.util.UUID
import scala.concurrent.ExecutionContext

object Bank {

  //Persistent Actors use:
  //commands
  //events
  //state
  //command handler
  //event handler
  //apply method
  import PersistentBankAccount.Command._
  import PersistentBankAccount.Command
  import PersistentBankAccount.Response._

  //events
  sealed trait Event
  case class BankAccountCreated(id: String) extends Event

  //state
  case class State(accounts: Map[String,ActorRef[Command]])

  //command handler
  def commandHandler (context: ActorContext[Command]) : (State,Command) => Effect[Event,State] = (state,commmand) => {
    commmand match {
      case createCommand @ CreateBankAccount(user, currency, initialBalance, replyTo) =>
        val id = UUID.randomUUID().toString
        val newBankAccount = context.spawn(PersistentBankAccount(id), id)
        Effect
        .persist(BankAccountCreated(id))
        .thenReply(newBankAccount)(_ => createCommand)

      case updateBalance @ UpdateBalance(id, currency, amount, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(updateBalance)
          case None =>
            Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None)) //failed to find account
        }


      case getCmd @ GetBankAccount(id,replyTo) => {
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(getCmd)
          case None =>
            Effect.reply(replyTo)(GetBankAccountResponse(None)) //failed to find account
      }


        }

    }
  }


  //event handler
  def eventHandler(context: ActorContext[Command]): (State,Event) => State = (state,event) =>
    event match {
      case BankAccountCreated(id) =>
        val account = context.child(id) //exists after command handler
                      .getOrElse(context.spawn(PersistentBankAccount(id), id)) //does not exist in recovery mode so it has to be created
          .asInstanceOf[ActorRef[Command]]
        state.copy(state.accounts + (id -> account))
    }

  //behavior
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event,State] (
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )

  }
}

/*
object BankPlayGround extends App {
  import PersistentBankAccount.Command._
  import PersistentBankAccount.Response._
  import PersistentBankAccount.Response

  val rootBehavior : Behavior[NotUsed] = Behaviors.setup { context =>
    val bank = context.spawn(Bank(), "bank")
    val logger = context.log

    //is this actually an actor??
    val responseHandler = context.spawn(Behaviors.receiveMessage[Response]{
      case BankAccountCreatedResponse(id) =>
        logger.info(s"succesfully created bank account $id")
        Behaviors.same
      case GetBankAccountResponse(maybeBankAccount) =>
        logger.info(s"Account details: $maybeBankAccount")
        Behaviors.same
    } , "replyHandler"  )

    //ask pattern
    import akka.actor.typed.scaladsl.AskPattern._
    import scala.concurrent.duration._
    implicit val timeout : Timeout = Timeout(2.seconds)
    implicit val scheduler : Scheduler = context.system.scheduler
    implicit val ec: ExecutionContext = context.executionContext

    //1) first run this to create an account
    //bank ! CreateBankAccount("daniel", "USD", 10, responseHandler)

    //2) secondly ,comment out command above , run this to verify that retrieval works
    //use the id from the log: [2022-09-21 14:28:54,849] [INFO] [como.tudux.bank.actors.BankPlayGround$] [BankDemo-akka.actor.default-dispatcher-6] [] - succesfully created bank account 39dc21d9-c404-4381-be08-b4ebb7c74095
    bank ! GetBankAccount("39dc21d9-c404-4381-be08-b4ebb7c74095",responseHandler)
    //result should be something like:
    // [2022-09-21 14:35:13,633] [INFO] [como.tudux.bank.actors.BankPlayGround$] [BankDemo-akka.actor.default-dispatcher-7] [] - Account details: Some(BankAccount(39dc21d9-c404-4381-be08-b4ebb7c74095,daniel,USD,10.0))

    /*
    bank.ask(replyTo => CreateBankAccount("daniel", "USD", 10, replyTo)).flatMap {
      case BankAccountCreatedResponse(id) =>
        context.log.info(s"succesful created bank account $id")
        bank.ask(replyTo => GetBankAccount(id,replyTo))
    }.foreach {
      case GetBankAccountResponse(maybeBankAccount) =>
        context.log.info(s"Account details: $maybeBankAccount")
    }*/
    Behaviors.empty
  }

  val system = ActorSystem(rootBehavior,"BankDemo")

}

*/

