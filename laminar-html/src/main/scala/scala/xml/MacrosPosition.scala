package scala.xml

import sourcecode.{File, FullName, Line}

case class MacrosPosition(file: File, line: Line, name: FullName) {
  override def toString: String = s"${file.value}:${line.value}"
}

object MacrosPosition {

  given generate(using file: File, line: Line, name: FullName): MacrosPosition =
    MacrosPosition(file, line, name)
}
