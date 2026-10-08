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
  <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#F0D98C"/><stop offset="0.3" stop-color="#D3A94A"/>
    <stop offset="0.6" stop-color="#9C7424"/><stop offset="0.85" stop-color="#C79B3E"/><stop offset="1" stop-color="#7C5A1B"/>
  </linearGradient>
  <linearGradient id="brassRev" x1="1" y1="1" x2="0" y2="0">
    <stop offset="0" stop-color="#F0D98C"/><stop offset="0.35" stop-color="#C79B3E"/>
    <stop offset="0.7" stop-color="#8E6820"/><stop offset="1" stop-color="#E3C375"/>
  </linearGradient>
  <radialGradient id="paperFace" cx="0.5" cy="0.45" r="0.6">
    <stop offset="0" stop-color="#FBF2D4"/><stop offset="0.7" stop-color="#F0DCA6"/><stop offset="1" stop-color="#DDBF7C"/>
  </radialGradient>
</defs>
