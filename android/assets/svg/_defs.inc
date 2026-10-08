<defs>
  <!-- Ink lines that wobble a little, like a dip pen on paper. -->
  <filter id="ink" filterUnits="userSpaceOnUse" x="-500" y="-500" width="3000" height="3000" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.035" numOctaves="2" seed="4" result="n"/>
    <feDisplacementMap in="SourceGraphic" in2="n" scale="5" xChannelSelector="R" yChannelSelector="G"/>
  </filter>
  <!-- Watercolour: uneven pigment, softly bleeding edges. -->
  <filter id="wash" filterUnits="userSpaceOnUse" x="-500" y="-500" width="3000" height="3000" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.012" numOctaves="3" seed="9" result="n"/>
    <feDisplacementMap in="SourceGraphic" in2="n" scale="9" xChannelSelector="R" yChannelSelector="G" result="d"/>
    <feGaussianBlur in="d" stdDeviation="0.9" result="b"/>
    <feTurbulence type="fractalNoise" baseFrequency="0.02" numOctaves="2" seed="21" result="p"/>
    <feColorMatrix in="p" type="matrix" values="0.26 0 0 0 0.87  0.26 0 0 0 0.87  0.26 0 0 0 0.87  0 0 0 0 1" result="shade"/>
    <feComposite in="shade" in2="b" operator="in" result="shadeIn"/>
    <feBlend in="shadeIn" in2="b" mode="multiply"/>
  </filter>
  <!-- A finer pen: less wobble, for the field-notebook look. -->
  <filter id="inkFine" filterUnits="userSpaceOnUse" x="-500" y="-500" width="3000" height="3000" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.07" numOctaves="2" seed="13" result="n"/>
    <feDisplacementMap in="SourceGraphic" in2="n" scale="2.6" xChannelSelector="R" yChannelSelector="G"/>
  </filter>
  <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#E7D299"/><stop offset="0.3" stop-color="#C4A460"/>
    <stop offset="0.6" stop-color="#8E6E36"/><stop offset="0.85" stop-color="#B8955A"/><stop offset="1" stop-color="#6E5226"/>
  </linearGradient>
  <linearGradient id="brassRev" x1="1" y1="1" x2="0" y2="0">
    <stop offset="0" stop-color="#E7D299"/><stop offset="0.35" stop-color="#B8955A"/>
    <stop offset="0.7" stop-color="#7A5C2C"/><stop offset="1" stop-color="#D9BF82"/>
  </linearGradient>
  <radialGradient id="paperFace" cx="0.5" cy="0.45" r="0.6">
    <stop offset="0" stop-color="#FBF2D4"/><stop offset="0.7" stop-color="#F0DCA6"/><stop offset="1" stop-color="#DDBF7C"/>
  </radialGradient>
  <!-- Pen hatching, used for shading. -->
  <pattern id="hatch" width="9" height="9" patternUnits="userSpaceOnUse" patternTransform="rotate(40)">
    <line x1="0" y1="0" x2="0" y2="9" stroke="#3B2A1A" stroke-width="1.3"/>
  </pattern>
  <linearGradient id="shadeFade" x1="0.15" y1="0.1" x2="0.9" y2="0.95">
    <stop offset="0.35" stop-color="#000"/><stop offset="1" stop-color="#fff"/>
  </linearGradient>
  <mask id="shadeMask" maskUnits="userSpaceOnUse" x="-500" y="-500" width="3000" height="3000">
    <rect x="-500" y="-500" width="3000" height="3000" fill="url(#shadeFade)"/>
  </mask>
</defs>
