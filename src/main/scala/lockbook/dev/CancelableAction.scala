package lockbook.dev

import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import scala.concurrent.duration.FiniteDuration

class CancelableAction(executor: ScheduledThreadPoolExecutor, delay: FiniteDuration, fun: () => Unit) {
  var currentTask: Option[ScheduledFuture[_]] = None

  def schedule(): Unit = {
    currentTask = Some(executor.schedule(CancelableAction.getRunnable(fun), delay.toMillis, TimeUnit.MILLISECONDS))
  }

  def doNow(): Unit = {
    cancel()
    currentTask = None
    executor.execute(CancelableAction.getRunnable(fun))
  }

  def snooze(): Unit = {
    cancel()
    schedule()
  }

  def cancel(): Unit = {
    currentTask.foreach(_.cancel(false))
  }
}

object CancelableAction {
  def apply(executor: ScheduledThreadPoolExecutor, delay: FiniteDuration, fun: () => Unit): CancelableAction =
    new CancelableAction(executor, delay, fun)

  def getRunnable(fun: () => Unit): Runnable = () => fun()
}
