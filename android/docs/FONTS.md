# Fonts

- **LXGW WenKai TC** (霞鶩文楷 繁體), SIL Open Font License 1.1, Copyright The LXGW WenKai Project Authors (https://github.com/lxgw/LxgwWenKai, as served by Google Fonts).
  `app/src/main/res/font/lxgw_wenkai_tc_subset.ttf` is a subset holding only the characters the console writes; to show more characters, subset the original
  font again with them (fontTools `pyftsubset`) and replace the file. Characters missing from the file fall back to the system font and then look out of place.
- Tried and left out: Ma Shan Zheng, Long Cang, Zhi Mang Xing, Liu Jian Mao Cao and ZCOOL XiaoWei (all Google Fonts, OFL): they lack traditional characters such
  as 區 環 島, which then show in another font.
