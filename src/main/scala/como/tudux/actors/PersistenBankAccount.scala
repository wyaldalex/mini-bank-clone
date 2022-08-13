package como.tudux.actors

import akka.actor.typed.ActorRef

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


}
