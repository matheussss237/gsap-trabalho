/*
 * Animação de entrada do Nexus Chat (GSAP).
 *
 * Comunicação com o JavaFX:
 *   Java → JS : startIntro(), setStatus(texto, erro), playExit()
 *   JS → Java : javaBridge.introDone(), javaBridge.finish()
 *
 * Sequência: fundo → partículas → rede neural → logo → nome (letra a letra)
 *            → slogan e chips → barra de progresso → (aguarda o Java) → saída.
 */

var WIDTH = 760;
var HEIGHT = 460;

function callJava(method) {
  if (window.javaBridge && typeof window.javaBridge[method] === "function") {
    window.javaBridge[method]();
  }
}

/* ---------- elementos gerados ---------- */

function createStars(count) {
  var container = document.getElementById("stars");
  for (var i = 0; i < count; i++) {
    var star = document.createElement("span");
    star.className = "star";
    star.style.left = Math.random() * 100 + "%";
    star.style.top = Math.random() * 100 + "%";
    container.appendChild(star);
  }
}

function createNetwork() {
  var ns = "http://www.w3.org/2000/svg";
  var links = document.getElementById("links");
  var nodes = document.getElementById("nodes");

  // pontos espalhados pelas bordas, evitando o centro onde fica o logo
  var points = [
    [70, 80], [180, 40], [120, 200], [60, 340], [200, 400], [250, 290],
    [690, 70], [580, 40], [640, 200], [700, 350], [560, 410], [520, 300]
  ];
  var pairs = [
    [0, 1], [0, 2], [1, 2], [2, 3], [3, 4], [2, 5], [4, 5],
    [6, 7], [6, 8], [7, 8], [8, 9], [9, 10], [8, 11], [10, 11]
  ];

  pairs.forEach(function (pair) {
    var a = points[pair[0]];
    var b = points[pair[1]];
    var line = document.createElementNS(ns, "line");
    line.setAttribute("x1", a[0]);
    line.setAttribute("y1", a[1]);
    line.setAttribute("x2", b[0]);
    line.setAttribute("y2", b[1]);
    var length = Math.sqrt(Math.pow(b[0] - a[0], 2) + Math.pow(b[1] - a[1], 2));
    line.setAttribute("stroke-dasharray", length);
    line.setAttribute("stroke-dashoffset", length);
    line.setAttribute("class", "link");
    links.appendChild(line);
  });

  points.forEach(function (p) {
    var circle = document.createElementNS(ns, "circle");
    circle.setAttribute("cx", p[0]);
    circle.setAttribute("cy", p[1]);
    circle.setAttribute("r", 4);
    circle.setAttribute("class", "node");
    nodes.appendChild(circle);
  });
}

function splitTitle() {
  var title = document.getElementById("title");
  var text = title.textContent;
  title.textContent = "";
  for (var i = 0; i < text.length; i++) {
    var span = document.createElement("span");
    span.className = "char";
    span.innerHTML = text[i] === " " ? "&nbsp;" : text[i];
    title.appendChild(span);
  }
}

/* ---------- animações ---------- */

var idleTweens = [];

function startIntro() {
  if (window.__started) return;
  window.__started = true;
  createStars(60);
  createNetwork();
  splitTitle();

  var logoStroke = document.querySelector(".logo-stroke");
  var strokeLength = logoStroke.getTotalLength();
  gsap.set(logoStroke, { strokeDasharray: strokeLength, strokeDashoffset: strokeLength });
  gsap.set(".card", { visibility: "visible" });

  var tl = gsap.timeline({
    defaults: { ease: "power3.out" },
    onComplete: function () {
      startIdle();
      callJava("introDone");
    }
  });

  tl.from(".card", { scale: 0.92, opacity: 0, duration: 0.6 })
    .from(".glow", { scale: 0.4, opacity: 0, duration: 1.2, stagger: 0.15 }, "<")
    .from(".star", { opacity: 0, scale: 0, duration: 0.6, stagger: { each: 0.01, from: "random" } }, "-=0.9")
    .from(".node", {
      attr: { r: 0 }, opacity: 0, duration: 0.5, ease: "back.out(3)",
      stagger: { each: 0.04, from: "random" }
    }, "-=0.5")
    .to(".link", { strokeDashoffset: 0, duration: 0.8, stagger: 0.03, ease: "power2.inOut" }, "-=0.4")

    // logo: entra girando e "quica"
    .from(".logo", { scale: 0, rotation: -120, duration: 0.9, ease: "back.out(1.8)" }, "-=0.6")
    .to(logoStroke, { strokeDashoffset: 0, duration: 0.7, ease: "power2.inOut" }, "-=0.35")
    .from(".logo-dot", {
      scale: 0, transformOrigin: "50% 50%", duration: 0.35, ease: "back.out(4)", stagger: 0.08
    }, "-=0.45")
    .fromTo(".logo-ring", { scale: 0.6, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.6 }, "-=0.3")

    // nome da aplicação, letra a letra
    .from(".title .char", {
      y: 40, opacity: 0, rotationX: -90, duration: 0.55, stagger: 0.045, ease: "back.out(2)"
    }, "-=0.3")

    // elementos secundários
    .from(".tagline", { y: 12, opacity: 0, duration: 0.5 }, "-=0.2")
    .from(".chips li", { y: 16, opacity: 0, scale: 0.8, duration: 0.4, stagger: 0.08 }, "-=0.25")
    .from(".loader", { opacity: 0, y: 10, duration: 0.4 }, "-=0.1")
    .from(".version", { opacity: 0, duration: 0.4 }, "<")
    .to("#bar-fill", { scaleX: 0.7, duration: 0.9, ease: "power1.inOut" }, "<");
}

/* animações contínuas enquanto o Java termina de conectar ao banco */
function startIdle() {
  idleTweens.push(gsap.to(".logo-ring", {
    scale: 1.12, opacity: 0.25, duration: 1.2, repeat: -1, yoyo: true, ease: "sine.inOut"
  }));
  idleTweens.push(gsap.to(".node", {
    opacity: 0.35, duration: 0.9, repeat: -1, yoyo: true, ease: "sine.inOut",
    stagger: { each: 0.12, from: "random" }
  }));
  idleTweens.push(gsap.to(".glow-a", {
    x: 60, y: 30, duration: 3, repeat: -1, yoyo: true, ease: "sine.inOut"
  }));
}

function setStatus(text, isError) {
  var status = document.getElementById("status");
  if (!status) return;
  gsap.to(status, {
    opacity: 0, duration: 0.15, onComplete: function () {
      status.textContent = text;
      status.className = isError ? "status error" : "status";
      gsap.to(status, { opacity: 1, duration: 0.2 });
    }
  });
}

/* animação de saída → transição para a tela de login */
function playExit() {
  idleTweens.forEach(function (t) { t.kill(); });

  var tl = gsap.timeline({
    defaults: { ease: "power2.in" },
    onComplete: function () { callJava("finish"); }
  });

  tl.to("#bar-fill", { scaleX: 1, duration: 0.35, ease: "power2.out" })
    .to(".chips li, .tagline, .loader, .version", { y: 12, opacity: 0, duration: 0.3, stagger: 0.04 })
    .to(".title .char", { y: -24, opacity: 0, duration: 0.3, stagger: 0.025 }, "-=0.2")
    .to(".link", { opacity: 0, duration: 0.3 }, "<")
    .to(".node", { attr: { r: 0 }, duration: 0.3, stagger: { each: 0.02, from: "center" } }, "<")
    .to(".logo-ring", { scale: 2.4, opacity: 0, duration: 0.5, ease: "power2.out" }, "-=0.1")
    .to(".logo", { scale: 1.25, rotation: 8, duration: 0.25, ease: "power2.out" }, "<")
    .to(".logo", { scale: 0, rotation: -40, opacity: 0, duration: 0.4 })
    .to(".card", { opacity: 0, scale: 1.04, duration: 0.4 }, "-=0.2");
}

/* Abrir o arquivo direto no navegador (sem JavaFX) também executa a animação. */
window.addEventListener("load", function () {
  setTimeout(function () {
    if (!window.__javafx && !window.__started) {
      startIntro();
      setTimeout(playExit, 5500);
    }
  }, 600);
});
