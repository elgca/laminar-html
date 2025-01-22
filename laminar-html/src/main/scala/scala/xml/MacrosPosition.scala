package scala.xml

import sourcecode.{File, FullName, Line, Name}

case class MacrosPosition(file: File, line: Line, name: FullName, showNane: Name) {
  override def toString: String = s"${file.value}:${line.value}"
}

object MacrosPosition {

  given generate(using file: File, line: Line, name: FullName, showName: Name): MacrosPosition =
    MacrosPosition(file, line, name, showName)
}
