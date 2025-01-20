package example

import com.raquo.airstream.state.Var

def SearchInput(searchText: Var[String]) = {
  <div class="input-icon">
      <span class="input-icon-addon"> <!-- Download SVG icon from http://tabler-icons.io/i/search -->
        <svg xmlns="http://www.w3.org/2000/svg"
             class="icon" width="24" height="24" viewBox="0 0 24 24"
             stroke-width="2" stroke="currentColor" fill="none"
             stroke-linecap="round" stroke-linejoin="round">
          <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
          <path d="M10 10m-7 0a7 7 0 1 0 14 0a7 7 0 1 0 -14 0" />
          <path d="M21 21l-6 -6" />
        </svg>
      </span>
    <input type="text"
           value={searchText}
           input={(value: String) => searchText.set(value)}
           class="form-control"
           placeholder="Search…"
           aria-label="Search"
    />
  </div>
}

def ContactItem(person: Person) = {
  <a href={s"#chat-${person.id}"}
     class="nav-link text-start mw-100 p-3 active"
     id="chat-1-tab"
     data-bs-toggle="pill"
     role="tab"
     aria-selected="true">
    <div class="row align-items-center flex-fill">
      <div class="col-auto"><span class="avatar" style={
        s"background-image: url(${person.avatarUrl})"
      }></span>
      </div>
      <div class="col text-body">
        <div>{person.name}</div>
        <div class="text-secondary text-truncate w-100">{person.bio}</div>
      </div>
    </div>
  </a>
}

def ContactList = {
  val searchText: Var[String] = Var("")
  val personList              = Var(GetPersons)

  val searchSignal = searchText.signal

  val items = personList.signal
    .map(seq => {
      searchSignal
        .map(_.toLowerCase)
        .map { key =>
          if (key.isEmpty) seq
          else seq.filter(p => p.bio.toLowerCase.contains(key) || p.name.toLowerCase.contains(key))
        }
    })
    .flattenSwitch
    .map(_.map(ContactItem))

  <div class="col-12 col-lg-5 col-xl-3 border-end">
    <div class="card-header d-none d-md-block">
      {SearchInput(searchText)}
    </div>
    <div class="card-body p-0 scrollable" style="max-height: 35rem">
      <div class="nav flex-column nav-pills" role="tablist">
        {items}
      </div>
    </div>
  </div>
}
