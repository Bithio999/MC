/* Offline search and responsive navigation: no fetches or third-party scripts. */
document.documentElement.classList.add('js-ready');
const toggle = document.getElementById('navigation-toggle');
const navigation = document.getElementById('wiki-navigation');
toggle.addEventListener('click', () => {
  const expanded = toggle.getAttribute('aria-expanded') !== 'true';
  toggle.setAttribute('aria-expanded', String(expanded));
  navigation.classList.toggle('is-open', expanded);
});
const input = document.getElementById('wiki-search');
const results = document.getElementById('search-results');
const status = document.getElementById('search-status');
let active = -1;
function closeResults() {
  results.hidden = true;
  input.setAttribute('aria-expanded', 'false');
  input.removeAttribute('aria-activedescendant');
  active = -1;
}
function search() {
  results.replaceChildren();
  active = -1;
  input.removeAttribute('aria-activedescendant');
  const query = input.value.trim().toLocaleLowerCase();
  if (!query) { closeResults(); status.textContent = ''; return; }
  const scope = document.body.dataset.mod || 'all';
  const matches = (window.WIKI_PAGES || []).filter(page => scope === 'all' || page.mod === scope).map(page => ({page,
    score: page.title.toLocaleLowerCase() === query ? 4 : page.title.toLocaleLowerCase().includes(query) ? 3 :
      (page.keywords || '').toLocaleLowerCase().includes(query) ? 1 : 0
  })).filter(match => match.score).sort((a, b) => b.score - a.score).slice(0, 8);
  for (const {page} of matches) {
    const anchor = document.createElement('a');
    anchor.href = document.body.dataset.root + 'site/' + page.slug + '.html' + (page.anchor ? '#' + page.anchor : '');
    anchor.textContent = page.title;
    if (scope === 'all') { const badge = document.createElement('small'); badge.className = 'search-result-mod'; badge.textContent = ({myths:'Tensura Myths',additions:'TensuraAdditionalAdditions',retired_addon:'Tensura retired_addon'}[page.mod] || 'Addon'); anchor.append(badge); }
    anchor.id = 'search-result-' + results.children.length;
    anchor.setAttribute('role', 'option');
    anchor.setAttribute('aria-selected', 'false');
    results.append(anchor);
  }
  if (!matches.length) {
    const message = document.createElement('p');
    message.textContent = 'No matching articles.';
    results.append(message);
  }
  results.hidden = false;
  input.setAttribute('aria-expanded', 'true');
  status.textContent = matches.length ? matches.length + ' matching articles. Use arrow keys to choose.' : 'No matching articles.';
}
input.addEventListener('input', search);
input.addEventListener('focus', () => { if (input.value.trim()) search(); });
input.addEventListener('keydown', event => {
  if (event.key === 'Escape') { closeResults(); return; }
  const options = [...results.querySelectorAll('a')];
  if (event.key === 'Enter' && !results.hidden && options.length) {
    event.preventDefault(); options[Math.max(active, 0)].click();
  }
  if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && options.length) {
    event.preventDefault();
    results.hidden = false;
    input.setAttribute('aria-expanded', 'true');
    active = (active + (event.key === 'ArrowDown' ? 1 : -1) + options.length) % options.length;
    options.forEach((option, index) => option.setAttribute('aria-selected', String(index === active)));
    input.setAttribute('aria-activedescendant', options[active].id);
    options[active].scrollIntoView({block: 'nearest'});
  }
});
document.addEventListener('click', event => { if (!event.target.closest('.search')) closeResults(); });
