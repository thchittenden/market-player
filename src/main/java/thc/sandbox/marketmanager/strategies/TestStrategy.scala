package thc.sandbox.marketmanager.strategies

import thc.sandbox.slf4s.Logger
import akka.actor.Props
import akka.actor.ActorRef

object TestStrategy {
	val constructor = (ar: ActorRef) => Props(new TestStrategy(ar))
}

class TestStrategy(val orderDispatcher: ActorRef) extends Strategy with Logger {

	def receive = {
		case m => logger.info(s"received message $m")
	}
	
}