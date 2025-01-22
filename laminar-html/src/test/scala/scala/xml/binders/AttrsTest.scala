package scala.xml.binders

import utest.TestSuite

object AttrsTest {

  def main(args: Array[String]): Unit = {
    Attrs.attributeDefine.value.foreach(println)
  }
}
