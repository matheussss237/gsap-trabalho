gsap.registerPlugin(ScrollTrigger);

// animacao de entrada
let tl = gsap.timeline();

tl.from(".intro-titulo", {
  y: 50,
  opacity: 0,
  duration: 1
});

tl.to(".intro", {
  y: "-100%",
  duration: 1,
  delay: 0.5
});

tl.from("nav", {
  y: -50,
  opacity: 0,
  duration: 0.5
});

tl.from(".sol", {
  scale: 0,
  duration: 1
});

tl.from(".titulo", {
  x: -100,
  opacity: 0,
  duration: 0.8
});

tl.from(".subtitulo", {
  opacity: 0,
  duration: 0.5
});

// sol girando e pulsando
gsap.to(".sol", {
  scale: 1.1,
  duration: 1.5,
  repeat: -1,
  yoyo: true
});

// animacao no scroll dos planetas
let planetas = document.querySelectorAll(".planeta-box");

planetas.forEach(function (box) {
  let planeta = box.querySelector(".planeta");
  let texto = box.querySelector(".texto");

  gsap.from(planeta, {
    x: -200,
    rotation: -180,
    opacity: 0,
    duration: 1,
    scrollTrigger: {
      trigger: box,
      start: "top 70%",
      end: "top 20%",
      scrub: 1
    }
  });

  gsap.from(texto, {
    x: 200,
    opacity: 0,
    duration: 1,
    scrollTrigger: {
      trigger: box,
      start: "top 70%",
      end: "top 20%",
      scrub: 1
    }
  });
});

// texto final
gsap.from(".fim-titulo", {
  scale: 0,
  duration: 1,
  scrollTrigger: {
    trigger: "#fim",
    start: "top 60%"
  }
});
