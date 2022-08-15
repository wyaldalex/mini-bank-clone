package como.tudux.bank.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

//a single bank account
//bottom of the category, meaning some child actor
//Event sourcing for: fault tolerance and auditing
class PersistenBankAccount {

  //commands = messages
  sealed trait Command
  //WARNING the use of Doubles is discouraged for real apps as Double is not reliable
  case class CreateBankAccount(user: String, currency: String, initialBalance: Double, replyTo: ActorRef[Response]) extends Command
  case class UpdateBalance(id: String,currency: String,amount: Double,replyTo: ActorRef[Response]) extends Command
  case class GetBankAccount(id: String, replyTo: ActorRef[Response]) extends Command
  //events = to persist to the db
  sealed  trait Event
  case class BankAccountCreated(bankAccount: BankAccount) extends Event
  case class BalanceUpdated(amount: Double) extends Event

  //state
  case class BankAccount(id: String, user: String, currency: String, balance: Double)
  //responses
  sealed trait Response
  case class BankAccountCreatedResponse(id: String) extends Response
  case class BankAccountBalanceUpdatedResponse(maybeBankAccount: Option[BankAccount]) extends Response
  case class GetBankAccountResponse(maybeBankAccount: Option[BankAccount]) extends Response

  //Defining a persistent actor
  //command handler = message handler => persist an event
  //state

  val commandHandler: (BankAccount,Command) => Effect[Event, BankAccount] = (state, command) =>
    command match {
      case CreateBankAccount(user, currency, initialBalance, replyTo) =>
        val id = state.id
        /*
          -bank creates me
          -bank sends me CreatedBankAccount
          -I persist BankAccountCreated
          -I update my state
          -reply bank to my bank with BankAccountCreatedResponse
          -(the bank surfaces the response to the HTTP server)
         */
        Effect
           .persist(BankAccountCreated(BankAccount(id , user , currency , initialBalance)))
           .thenReply(replyTo)(_ => BankAccountCreatedResponse(id))
      case UpdateBalance(_, _, amount, replyTo) =>
        val newBalance = state.balance + amount
        //check here for withdrawal
        if(newBalance < 0) //illegal
          Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None))
        else
          Effect
            .persist(BalanceUpdated(amount))
            .thenReply(replyTo)(newState => BankAccountBalanceUpdatedResponse(Some(newState)))
      case GetBankAccount(_, replyTo) =>
        Effect.reply(replyTo)(GetBankAccountResponse(Some(state)))
    }

  val eventHandler: (BankAccount, Event) => BankAccount = (state,event) =>
    event match {
      case BankAccountCreated(bankAccount) =>
        bankAccount
      case BalanceUpdated(amount) =>
        state.copy(balance = state.balance + amount)
    }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, BankAccount] (
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = BankAccount(id,"","",0.0),
      commandHandler = ,
      eventHandler =


    )



}
