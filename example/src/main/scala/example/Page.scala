package example

import com.raquo.laminar.api.L.Element

import scala.xml.Elem

def header(name: String) = {
  <div class="page-header d-print-none">
    <div class="container-xl">
      <div class="row g-2 align-items-center">
        <div class="col">
          <h2 class="page-title">
            {name}
          </h2>
        </div>
      </div>
    </div>
  </div>
}

def bodyContainer(body: Element) = {
  <div class="page-body">
    <div class="container-xl">
      {body}
    </div>
  </div>
}

def page = {
  <div class="page-wrapper">
    {header("Chart")}
    {bodyContainer(chart)}
  </div>
}
