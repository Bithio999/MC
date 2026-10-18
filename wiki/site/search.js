"use strict";
const input = document.getElementById("wiki-search");
const results = document.getElementById("search-results");
input.addEventListener("input", () => {
  results.replaceChildren();
  const query = input.value.trim().toLowerCase();
  if (!query) return;
  const matches = window.WIKI_PAGES.filter(page => page.title.toLowerCase().includes(query));
  for (const page of matches.slice(0, 8)) {
    const link = document.createElement("a");
    link.href = document.body.dataset.root + "site/" + page.slug + ".html";
    link.textContent = page.title;
    results.append(link);
  }
  if (!matches.length) results.textContent = "No matching articles.";
});
document.addEventListener("keydown", event => {
  if (event.key === "Escape") { input.value = ""; results.replaceChildren(); }
});
