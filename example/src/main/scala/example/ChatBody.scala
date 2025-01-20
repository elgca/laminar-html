package example

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import org.scalajs.dom

def ChartItem(msg: String) = {
  <div class="chat-item">
    {L.onMountCallback(ctx => ctx.thisNode.ref.scrollIntoView())}
    <div class={"row align-items-end justify-content-end"}>
      <div class="col col-lg-6">
        <div class="chat-bubble chat-bubble-me">
          <div class="chat-bubble-title">
            <div class="row">
              <div class="col chat-bubble-author">Paweł Kuna</div>
              <div class="col-auto chat-bubble-date">09:32</div>
            </div>
          </div>
          <div class="chat-bubble-body">
            <p>{msg}</p>
          </div>
        </div>
      </div>
      <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
      </div>
    </div>
  </div>
}

def ChartBody = {
  val msgValue  = Var(Vector.empty[String])
  val msageList = msgValue.splitByIndex((id, msg, varmsg) => ChartItem(msg))

  <div class="col-12 col-lg-7 col-xl-9 d-flex flex-column">
    <div class="card-body scrollable" style="height: 35rem">
      <div class="chat">
        <div class="chat-bubbles">
          <div class="chat-item">
            <div class="row align-items-end justify-content-end">
              <div class="col col-lg-6">
                <div class="chat-bubble chat-bubble-me">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Paweł Kuna</div>
                      <div class="col-auto chat-bubble-date">09:32</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Hey guys, I just pushed a new commit on the <code>dev</code> branch. Can you have a look and tell me what you think?</p>
                  </div>
                </div>
              </div>
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/002m.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Mallory Hulme</div>
                      <div class="col-auto chat-bubble-date">09:34</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Sure Paweł, let me pull the latest updates.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/003m.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Dunn Slane</div>
                      <div class="col-auto chat-bubble-date">09:34</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>I'm on it too 👊</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/002m.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Mallory Hulme</div>
                      <div class="col-auto chat-bubble-date">09:40</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>I see you've refactored the <code>calculateStatistics</code> function. The code is much cleaner now.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end justify-content-end">
              <div class="col col-lg-6">
                <div class="chat-bubble chat-bubble-me">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Paweł Kuna</div>
                      <div class="col-auto chat-bubble-date">09:42</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Yes, I thought it was getting a bit cluttered.</p>
                  </div>
                </div>
              </div>
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000f.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Emmy Levet</div>
                      <div class="col-auto chat-bubble-date">09:43</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>The commit message is descriptive, too. Good job on mentioning the issue number it fixes.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/003m.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Dunn Slane</div>
                      <div class="col-auto chat-bubble-date">09:44</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>I noticed you added some new dependencies in the <code>package.json</code>. Did you also update the <code>README</code> with the setup instructions?</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end justify-content-end">
              <div class="col col-lg-6">
                <div class="chat-bubble chat-bubble-me">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Paweł Kuna</div>
                      <div class="col-auto chat-bubble-date">09:45</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Oops, I forgot. I'll add that right away.</p>
                    <div class="mt-2">
                      <img src="https://media3.giphy.com/media/VABbCpX94WCfS/giphy.gif" alt="" class="rounded img-fluid" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/002m.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Mallory Hulme</div>
                      <div class="col-auto chat-bubble-date">09:46</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>I see a couple of edge cases we might not be handling in the <code>calculateStatistic</code> function. Should I open an issue for that?</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end justify-content-end">
              <div class="col col-lg-6">
                <div class="chat-bubble chat-bubble-me">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Paweł Kuna</div>
                      <div class="col-auto chat-bubble-date">09:47</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Yes, Bob. Please do. We should not forget to handle those.</p>
                  </div>
                </div>
              </div>
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000f.jpg)"></span>
              </div>
              <div class="col col-lg-6">
                <div class="chat-bubble">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Emmy Levet</div>
                      <div class="col-auto chat-bubble-date">09:50</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Alright, once the <code>README</code> is updated, I'll merge this commit into the main branch. Nice work, Paweł.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end justify-content-end">
              <div class="col col-lg-6">
                <div class="chat-bubble chat-bubble-me">
                  <div class="chat-bubble-title">
                    <div class="row">
                      <div class="col chat-bubble-author">Paweł Kuna</div>
                      <div class="col-auto chat-bubble-date">09:52</div>
                    </div>
                  </div>
                  <div class="chat-bubble-body">
                    <p>Thanks, <a href="#">@everyone</a>! 🙌</p>
                  </div>
                </div>
              </div>
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000m.jpg)"></span>
              </div>
            </div>
          </div>
          <div class="chat-item">
            <div class="row align-items-end">
              <div class="col-auto"><span class="avatar" style="background-image: url(./static/avatars/000f.jpg)"></span>
              </div>
              <div class="col-auto">
                <div class="chat-bubble">
                  <div class="chat-bubble-body">
                    <p class="text-secondary text-italic">typing<span class="animated-dots"></span></p>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
                       msgValue.update(_.appended(value))
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
