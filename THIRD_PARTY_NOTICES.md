# Third-party notices

Gearscope is licensed under the MIT License (see [LICENSE](LICENSE)). Parts of its source code are adapted
from the projects listed below, which are also licensed under the MIT License. As required by that license,
their copyright notices and license texts are reproduced here.

Gearscope is an unofficial companion and is not affiliated with these projects.

## Apothic Attributes

- Project: https://github.com/Shadows-of-Fire/Apothic-Attributes (by Shadows_of_Fire)
- Version referenced: 1.21.1-2.11.0
- Adapted code in Gearscope:
  - `com.drehverschluss.gearscope.compat.apothic.ApothicAttributeSupport` – the attribute tooltip follows
    `dev.shadowsoffire.apothic_attributes.client.AttributesGui#renderTooltip` step by step.
  - `com.drehverschluss.gearscope.client.attributes.AttributeListPanel#valueColor` – follows the value color
    logic of `AttributesGui#getValueColor`.

```
MIT License

Copyright (c) 2023-2025 Stormraven Studios, LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Accessories

- Project: https://github.com/wisp-forest/accessories (by Blodhgarm, chyzman, Dr.Zeal)
- Version referenced: 1.1.0-beta.54+1.21.1
- Adapted code in Gearscope:
  - `com.drehverschluss.gearscope.compat.accessories.GearscopeAccessorySlot` – cosmetic slot handling
    (`mayPlace`, `mayPickup`) and the empty slot tooltip follow
    `io.wispforest.accessories.menu.AccessoriesInternalSlot`.

```
MIT License

Copyright (c) 2024 Wisp Forest

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
