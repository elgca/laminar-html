import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / organization         := "io.github.elgca"
ThisBuild / organizationName     := "elgca"
ThisBuild / organizationHomepage := Some(url("https://github.com/elgca"))
ThisBuild / versionScheme        := Some("early-semver")
//ThisBuild / scmInfo              := Some(
//  ScmInfo(
//    url("https://github.com/elgca/laminar-html"),
//    "scm:git@github.com:elgca/laminar-html.git",
//  ),
//)
ThisBuild / description          := "elgca"
ThisBuild / licenses             := Seq(("MIT", url("http://opensource.org/licenses/MIT")))
ThisBuild / homepage             := Some(url("https://github.com/elgca/laminar-html"))
ThisBuild / developers           := List(
  Developer(
    "elgca",
    "kewenchao",
    "modtekent@live.com",
    url("https://github.com/elgca"),
  ),
)

// 从 POM 中删除除 Maven Central 之外的所有其他存储库
ThisBuild / publishTo := sonatypePublishTo.value
