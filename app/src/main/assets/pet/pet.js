(() => {
  'use strict';

  const pet = document.getElementById('pet');
  const sprite = document.getElementById('sprite');
  const bubble = document.getElementById('bubble');
  const validStates = new Set(['idle', 'blink', 'tap', 'rapid-tap', 'drag', 'sleep', 'perch']);
  const images = {
    idle: 'images/pet_base.png',
    blink: 'images/pet_blink.png',
    tap: 'images/pet_base.png',
    'rapid-tap': 'images/pet_base.png',
    drag: 'images/pet_base.png',
    sleep: 'images/pet_blink.png',
    perch: 'images/pet_perch.png'
  };
  let bubbleTimer;
  let stateTimer;

  function render(event = {}) {
    const state = validStates.has(event.state) ? event.state : 'idle';
    const perch = event.perch === true || state === 'perch';
    const visualState = state === 'perch' ? 'idle' : state;
    pet.className = `pet state-${visualState}${perch ? ' mode-perch' : ''}`;
    sprite.src = perch && visualState === 'idle' ? images.perch : images[visualState];
    clearTimeout(stateTimer);
    if (['blink', 'tap', 'rapid-tap', 'drag'].includes(visualState)) {
      stateTimer = setTimeout(() => {
        pet.className = `pet state-idle${perch ? ' mode-perch' : ''}`;
        sprite.src = perch ? images.perch : images.idle;
      }, visualState === 'drag' ? 900 : 650);
    }

    clearTimeout(bubbleTimer);
    const message = typeof event.bubble === 'string' ? event.bubble : '';
    bubble.textContent = message;
    bubble.hidden = message.length === 0;
    bubble.classList.toggle('is-visible', message.length > 0);

    if (message.length > 0) {
      const duration = Math.min(15_000, Math.max(500, Number(event.durationMs) || 4_000));
      bubbleTimer = setTimeout(() => {
        bubble.hidden = true;
        bubble.classList.remove('is-visible');
      }, duration);
    }
  }

  window.petEngine = Object.freeze({ render });
})();
