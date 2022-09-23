package como.tudux.bank.http


import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.util.Timeout
import como.tudux.bank.actors.PersistentBankAccount.{Command, Response}
import como.tudux.bank.actors.PersistentBankAccount.Command._
import como.tudux.bank.actors.PersistentBankAccount.Response.BankAccountCreatedResponse
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


case class BankAccountCreationRequest(user: String, currency: String, balance: Double) {
  def toCommand(replyTo: ActorRef[Response]) : Command = CreateBankAccount(user, currency,balance, replyTo)
}

class BankRoutes(bank: ActorRef[Command])(implicit system: ActorSystem[_]) {
  implicit val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreationRequest) : Future[Response] = {
    //this is were we call our persistent actor
    bank.ask(replyTo => request.toCommand(replyTo))
  }
  /*
  POST /bank/
       Payload: bank account request creation as JSON
       Response:
          201 Created
          Location /bank/uuid
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
      }
    }


}
