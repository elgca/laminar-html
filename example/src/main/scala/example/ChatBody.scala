package example

import com.raquo.airstream.state.Var
import org.scalajs.dom

import scala.scalajs.js.Date
import scala.xml.Elem

def MsgItem(
  userName: String,
  isMe: Boolean,
  time: Date,
  msg: Elem,
) = {
  <div class="col col-lg-6">
    <div class={s"chat-bubble ${if isMe then "chat-bubble-me" else ""}"}>
      <div class="chat-bubble-title">
        <div class="row">
          <div class="col chat-bubble-author">{userName}</div>
          <div class="col-auto chat-bubble-date">{time.toLocaleTimeString()}</div>
        </div>
      </div>
      <div class="chat-bubble-body">
        {msg}
      </div>
    </div>
  </div>
}

def ChartItem(chatMsg: ChatMsg) = {
  val userName = chatMsg.userName
  val isMe     = chatMsg.isMe
  val time     = chatMsg.time
  val avatar   = chatMsg.avatarUrl
  val msg      = chatMsg.msg

  def avatarElem = {
    <div class="col-auto">
      <span class="avatar" style={s"background-image: url(${avatar})"}></span>
    </div>
  }

  <div class="chat-item"
    onmount={(ref: dom.html.Element) => ref.scrollIntoView()}
  >
    <div class={s"row align-items-end ${if isMe then "justify-content-end" else ""}"}>
      {
        if !isMe then Some(avatarElem) else None
      }
      {
        MsgItem(
          userName = userName,
          isMe = isMe,
          time = time,
          msg = { <p onmount={(ref: dom.html.Element) => ref.innerHTML = msg} /> })
      }
      {
        if isMe then Some(avatarElem) else None
      }
    </div>
  </div>
}

def ChartBody = {
  val msgValue  = Var(GetChatHistory)
  val msageList = msgValue.splitByIndex((id, msg, varmsg) => ChartItem(msg))

  <div class="col-12 col-lg-7 col-xl-9 d-flex flex-column">
    <div class="card-body scrollable" style="height: 35rem">
      <div class="chat">
        <div class="chat-bubbles">
          {msageList}
        </div>
      </div>
    </div>
    <div class="card-footer">
      <div class="input-group input-group-flat">
        {
          <input type="text"
                 class="form-control"
                 autocomplete="off"
                 placeholder="Type message"
                 keydown={ (event: dom.KeyboardEvent) =>
                   {
                     if event.key == "Enter" then {
                       event.preventDefault();
                       val value = event.target.asInstanceOf[dom.html.Input].value
                       event.target.asInstanceOf[dom.html.Input].value = ""
                       msgValue.update(
                         _.appended(
                           ChatMsg(
                             true,
                             "Sure Paweł",
                             avatarUrl = randomAvatar,
                             time = new Date(),
                             msg = value,
                           )))
                     }
                   }
                 }
          />
        }
        <span class="input-group-text">
            <a href="#" class="link-secondary" data-bs-toggle="tooltip" aria-label="Clear search" title="Clear search"> <!-- Download SVG icon from http://tabler-icons.io/i/mood-smile -->
              <svg xmlns="http://www.w3.org/2000/svg" class="icon" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0" /><path d="M9 10l.01 0" /><path d="M15 10l.01 0" /><path d="M9.5 15a3.5 3.5 0 0 0 5 0" /></svg>
            </a>
            <a href="#" class="link-secondary ms-2" data-bs-toggle="tooltip" aria-label="Add notification" title="Add notification"> <!-- Download SVG icon from http://tabler-icons.io/i/paperclip -->
              <svg xmlns="http://www.w3.org/2000/svg" class="icon" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M15 7l-6.5 6.5a1.5 1.5 0 0 0 3 3l6.5 -6.5a3 3 0 0 0 -6 -6l-6.5 6.5a4.5 4.5 0 0 0 9 9l6.5 -6.5" /></svg>
            </a>
          </span>
      </div>
    </div>
  </div>
}
