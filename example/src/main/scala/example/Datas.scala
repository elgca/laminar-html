package example

import scala.scalajs.js.Date
import scala.util.Random
import scala.xml.Elem

val AvatarsUrl = Seq(
  "000f",
  "000m",
  "001f",
  "001m",
  "002f",
  "002m",
  "003f",
  "003m",
  "004f",
  "004m",
  "005f",
  "005m",
  "006f",
  "006m",
  "007f",
  "007m",
  "008f",
  "008m",
  "009f",
  "009m",
  "010f",
  "010m",
  "011f",
  "011m",
  "012f",
  "012m",
).map(avatar => s"./static/avatars/${avatar}.jpg")

def randomAvatar = AvatarsUrl(Random.nextInt(AvatarsUrl.size))

val bios = Seq(
  "let me pull the latest updates.",
  "I'm on it too 👊",
  "I see you've refactored the calculateStatistics function. The code is much cleaner now.",
  "Yes, I thought it was getting a bit cluttered.",
  "The commit message is descriptive, too. Good job on mentioning the issue number it fixes.",
  "I noticed you added some new dependencies in the package.json. Did you also update the README with the setup instructions?",
  "Oops, I forgot. I'll add that right away.",
  "I see a couple of edge cases we might not be handling in the calculateStatistic function. Should I open an issue for that?",
  "Yes, Bob. Please do. We should not forget to handle those.",
  "Alright, once the README is updated, I'll merge this commit into the main branch. Nice work, Paweł.",
)

def randomBio = bios(Random.nextInt(bios.size))

val names = Seq(
  "Paweł Kuna",
  "Jeffie Lewzey",
  "Mallory Hulme",
  "Dunn Slane",
  "Emmy Levet",
  "Maryjo Lebarree",
  "Egan Poetz",
  "Kellie Skingley",
  "Christabel Charlwood",
  "Haskel Shelper",
)

case class Person(
  id: Int,
  avatarUrl: String,
  name: String,
  bio: String,
)

def GetPersons = names.zipWithIndex.map { case (name, id) =>
  Person(
    id = id,
    avatarUrl = randomAvatar,
    name = name,
    bio = randomBio,
  )
}

case class ChatMsg(
  isMe: Boolean,
  userName: String,
  avatarUrl: String,
  time: Date,
  msg: String,
)

def GetChatHistory = Seq(
  """<p>Hey guys, I just pushed a new commit on the <code>dev</code> branch. Can you have a look and tell me what you think?</p>""",
  """<p>Sure Paweł, let me pull the latest updates.</p>""",
  """<p>I'm on it too 👊</p>""",
  """<p>I see you've refactored the <code>calculateStatistics</code> function. The code is much cleaner now.</p>""",
  """<p>Yes, I thought it was getting a bit cluttered.</p>""",
  """<p>The commit message is descriptive, too. Good job on mentioning the issue number it fixes.</p>""",
  """<p>I noticed you added some new dependencies in the <code>package.json</code>. Did you also update the <code>README</code> with the setup instructions?</p>""",
  """<p>Oops, I forgot. I'll add that right away.</p><div class="mt-2"><img src="https://media3.giphy.com/media/VABbCpX94WCfS/giphy.gif" alt="" class="rounded img-fluid" /></div>""",
  """<p>I see a couple of edge cases we might not be handling in the <code>calculateStatistic</code> function. Should I open an issue for that?</p>""",
  """<p>Yes, Bob. Please do. We should not forget to handle those.</p>""",
  """<p>Alright, once the <code>README</code> is updated, I'll merge this commit into the main branch. Nice work, Paweł.</p>""",
  """<p>Thanks, <a href="#">@everyone</a>! 🙌</p>""",
  """<p class="text-secondary text-italic">typing<span class="animated-dots"></span></p>""",
).zipWithIndex.map { case (msg, id) =>
  if id % 2 == 0 then {
    ChatMsg(
      false,
      "Jeffie Lewzey",
      avatarUrl = randomAvatar,
      time = new Date(),
      msg = msg,
    )
  } else {
    ChatMsg(
      true,
      "Sure Paweł",
      avatarUrl = randomAvatar,
      time = new Date(),
      msg = msg,
    )
  }
}
