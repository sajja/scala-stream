package logpublisher

import java.util.Date

/**
  * Created by sajith on 5/30/17.
  */
object UserFactory {
  def randomString(length: Int) = {
    val r = new scala.util.Random
    val sb = new StringBuilder
    for (i <- 1 to length) {
      sb.append(r.nextPrintableChar)
    }
    sb.toString
  }

  def createUser(): User = {
    val r = new scala.util.Random(new Date().getTime)
    User(randomString(3), randomString(4))
  }
}
