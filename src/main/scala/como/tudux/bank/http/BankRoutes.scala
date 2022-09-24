package como.tudux.bank.http


import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.util.Timeout
import como.tudux.bank.actors.PersistentBankAccount.{Command, Response}
import como.tudux.bank.actors.PersistentBankAccount.Command._
import como.tudux.bank.actors.PersistentBankAccount.Response.{BankAccountCreatedResponse, GetBankAccountResponse}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


case class BankAccountCreationRequest(user: String, currency: String, balance: Double) {
  def toCommand(replyTo: ActorRef[Response]) : Command = CreateBankAccount(user, currency,balance, replyTo)
}

case class FailureResponse(reason: String)

class BankRoutes(bank: ActorRef[Command])(implicit system: ActorSystem[_]) {
  implicit val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreationRequest) : Future[Response] = {
    //this is were we call our persistent actor
    bank.ask(replyTo => request.toCommand(replyTo))
  }

  def getBankAccount(id: String) : Future[Response] =
    bank.ask(replyTo => GetBankAccount(id, replyTo))
  /*
  POST /bank/
       Payload: bank account request creation as JSON
       Response:
          201 Created
          Location /bank/uuid

   GET /bank/uuid
       Response:
          200 ok
          JSON repr of bank account details
   */
  val routes =
    pathPrefix("bank") {
      pathEndOrSingleSlash {
        post {
          //parse the payload
          entity(as[BankAccountCreationRequest]) { request =>
            /*
               -convert the request into a Command for the Bank Actor
               -send the command to the Bank
               -expect a reply
               -send back an HTTP response
             */
            onSuccess(createBankAccount(request)) {
              case BankAccountCreatedResponse(id) =>
                respondWithHeader(Location(s"/bank/$id")) {
                  complete(StatusCodes.Created)
                }
            }
          }

        }
      } ~
      path(Segment) { id =>
        /*
        - send command to the bank
        - expect reply
        */
        onSuccess(getBankAccount(id)) {
          case GetBankAccountResponse(Some(account)) =>
            complete(account)
          case GetBankAccountResponse(None) =>
            complete(StatusCodes.NotFound, FailureResponse(s"Bank account $id cannot be found"))

        }
      }
    }
}
