package scala.xml

import java.util.Locale

val StrictEventsFunction = System.getProperty("strict_event_function") != "false"

val ShowTypeHints = System.getProperty("show_type_hints") != "false"

val isChinese = Seq(Locale.CHINESE, Locale.CHINA, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE)
  .contains(Locale.getDefault)
