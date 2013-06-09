package thc.sandbox.marketmanager.strategies

import akka.actor.Actor
import akka.actor.ActorRef

abstract class Strategy extends Actor {
	val orderDispatcher: ActorRef
}