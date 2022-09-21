package como.tudux.bank.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import java.util.UUID

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

    }
  }


  //event handler
  val eventHandler: (State,Event) => State = ???

  //behavior
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event,State] (
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler
    )

  }




}
