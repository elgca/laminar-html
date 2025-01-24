package test

import org.scalajs.dom

object StrictFunction {

  val x = {
    <div>
      <button
        onclick={(event: dom.MouseEvent) => {}}
      />
      <button
        online={(event: dom.Event) => {}}
      />
    </div>
  }
}
