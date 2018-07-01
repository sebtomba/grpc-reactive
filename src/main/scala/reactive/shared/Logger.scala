package reactive.shared

import org.slf4j

object Logger {
  def apply(clazz: Class[_]): slf4j.Logger = slf4j.LoggerFactory.getLogger(clazz.getName.stripSuffix("$"))
}
