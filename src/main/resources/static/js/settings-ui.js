"use strict";

const sectionNav = document.querySelector(".settings-section-nav");
if (sectionNav) {
  const links = Array.from(sectionNav.querySelectorAll("a"));
  const sections = links.map(link => document.getElementById(link.hash.slice(1)));
  const desktop = window.matchMedia("(min-width: 1001px)");

  function updateSection() {
    if (!desktop.matches) return;
    const threshold = sectionNav.getBoundingClientRect().top + 5;
    let active = 0;
    sections.forEach((section, index) => {
      if (section.getBoundingClientRect().top <= threshold) active = index;
    });
    links.forEach((link, index) => {
      if (index === active) link.setAttribute("aria-current", "location");
      else link.removeAttribute("aria-current");
    });
  }

  window.addEventListener("scroll", updateSection, { passive: true });
  window.addEventListener("load", updateSection);
  desktop.addEventListener("change", updateSection);
  updateSection();
}
