package como.tudux.bank.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect

object Bank {

  //Persistent Actors use:
  //commands
  //events
  //state
  //command handler
  //event handler
  //apply method

  import PersistentBankAccount.Command

  //events
  sealed trait Event
  case class BankAccountCreated(id: String) extends Event

  //state
  case class State(accounts: Map[String,ActorRef[Command]])

  //command handler
  val commandHandler: (State,Command) => Effect[Event,State] = ???

  //event handler
  val eventHandler: (State,Event) => State = ???

  //behavior
  def apply(): Behavior[Command] = ???




}
