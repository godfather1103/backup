package wps;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.XML;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WpsImgUtil {

    /**
     * 获取wps中的图片
     * 包括嵌入形式图片和浮动形式图片
     * <p>
     * 嵌入形式图片返回方式:
     * 以map方式返回
     * 键为行列格式 =DISPIMG("ID",1) 字符串
     * <p>
     * 浮动形式图片返回方式:
     * 以map方式返回
     * 键为行列格式 x-y  字符串
     *
     * @param dispStrList
     * @param simpleFile
     * @return
     * @throws IOException
     */
    public static Map<String, WpsImg> getWpsImgs(List<String> dispStrList, MultipartFile simpleFile) throws IOException {
        List<WpsImg> wpsImgList = new ArrayList<>();
        for (String dispStr : dispStrList) {
            if (Objects.nonNull(dispStr) && dispStr.startsWith("=DISPIMG")) {
                int start = dispStr.indexOf("\"");
                int end = dispStr.lastIndexOf("\"");
                if (start != -1 && end != -1) {
                    String imgId = dispStr.substring(start + 1, end);
                    WpsImg wpsImg = new WpsImg();
                    wpsImg.setType(0);
                    wpsImg.setImgId(imgId);
                    wpsImg.setCellStr(dispStr);
                    wpsImgList.add(wpsImg);
                }
            }
        }

        ZipInputStream zis = new ZipInputStream(simpleFile.getInputStream());
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    final String fileName = entry.getName();
                    if (Objects.equals(fileName, "xl/cellimages.xml")) {
                        String content = IOUtils.toString(zis, CharsetUtil.UTF_8);
                        JSONObject js = XML.toJSONObject(content);
                        if (Objects.isNull(js)) {
                            continue;
                        }
                        JSONObject cellImages = js.getJSONObject("etc:cellImages");
                        if (Objects.isNull(cellImages)) {
                            continue;
                        }
                        JSONArray cellImage = null;
                        try {
                            cellImage = cellImages.getJSONArray("etc:cellImage");
                        } catch (Exception e) {
                        }
                        if (Objects.isNull(cellImage)) {
                            JSONObject cellImageObj = null;
                            try {
                                cellImageObj = cellImages.getJSONObject("etc:cellImage");
                            } catch (Exception e) {
                            }
                            if (Objects.nonNull(cellImageObj)) {
                                cellImage = new JSONArray();
                                cellImage.add(cellImageObj);
                            }
                        }
                        if (Objects.isNull(cellImage)) {
                            continue;
                        }
                        for (int i = 0; i < cellImage.size(); i++) {
                            JSONObject imageItem = cellImage.getJSONObject(i);
                            if (Objects.isNull(imageItem)) {
                                continue;
                            }
                            JSONObject pic = imageItem.getJSONObject("xdr:pic");
                            if (Objects.isNull(pic)) {
                                continue;
                            }
                            JSONObject nvPicPr = pic.getJSONObject("xdr:nvPicPr");
                            if (Objects.isNull(nvPicPr)) {
                                continue;
                            }
                            JSONObject cNvPr = nvPicPr.getJSONObject("xdr:cNvPr");
                            if (Objects.isNull(cNvPr)) {
                                continue;
                            }
                            String name = cNvPr.getStr("name");
                            if (StringUtils.isNotEmpty(name)) {
                                WpsImg wpsImg = wpsImgList.stream().filter(item -> Objects.equals(item.getImgId(), name)).findFirst().orElse(null);
                                if (Objects.nonNull(wpsImg)) {
                                    JSONObject blipFill = pic.getJSONObject("xdr:blipFill");
                                    if (Objects.isNull(blipFill)) {
                                        continue;
                                    }
                                    JSONObject blip = blipFill.getJSONObject("a:blip");
                                    if (Objects.isNull(blip)) {
                                        continue;
                                    }
                                    String embed = blip.getStr("r:embed");
                                    wpsImg.setRId(embed);
                                }
                            }
                        }
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        } finally {
            zis.close();
        }
        ZipInputStream fzis = new ZipInputStream(simpleFile.getInputStream());
        try {
            ZipEntry entry;
            while ((entry = fzis.getNextEntry()) != null) {
                try {
                    final String fileName = entry.getName();
                    if (Objects.equals(fileName, "xl/_rels/cellimages.xml.rels")) {
                        String content = IOUtils.toString(fzis, CharsetUtil.UTF_8);
                        JSONObject js = XML.toJSONObject(content);
                        JSONObject relationships = js.getJSONObject("Relationships");
                        if (Objects.isNull(relationships)) {
                            continue;
                        }
                        JSONArray relationship = null;
                        try {
                            relationship = relationships.getJSONArray("Relationship");
                        } catch (Exception e) {

                        }
                        if (Objects.isNull(relationship)) {
                            try {
                                JSONObject relationshipObj = relationships.getJSONObject("Relationship");
                                if (Objects.nonNull(relationshipObj)) {
                                    relationship = new JSONArray();
                                    relationship.add(relationshipObj);
                                }
                            } catch (Exception e) {

                            }
                        }
                        if (Objects.isNull(relationship)) {
                            continue;
                        }
                        for (int i = 0; i < relationship.size(); i++) {
                            JSONObject relaItem = relationship.getJSONObject(i);
                            if (Objects.isNull(relaItem)) {
                                continue;
                            }
                            String id = relaItem.getStr("Id");
                            String target = "/xl/" + relaItem.getStr("Target");
                            if (StringUtils.isNotEmpty(id)) {
                                WpsImg wpsImg = wpsImgList.stream().filter(item -> Objects.equals(item.getRId(), id)).findFirst().orElse(null);
                                if (Objects.nonNull(wpsImg)) {
                                    wpsImg.setImgName(target);
                                }
                            }
                        }
                    }
                } finally {
                    fzis.closeEntry();
                }
            }
        } finally {
            fzis.close();
        }

        Workbook workbook = WorkbookFactory.create(simpleFile.getInputStream());
        List<XSSFPictureData> allPictures = (List<XSSFPictureData>) workbook.getAllPictures();
        for (XSSFPictureData pictureData : allPictures) {
            PackagePartName partName = pictureData.getPackagePart().getPartName();
            URI uri = partName.getURI();
            WpsImg wpsImg = wpsImgList.stream().filter(i -> Objects.equals(i.getImgName(), uri.toString())).findFirst().orElse(null);
            if (Objects.nonNull(wpsImg)) {
                wpsImg.setPictureData(pictureData);
            }
        }
        Map<String, WpsImg> result = new HashMap<>();
        for (WpsImg wpsImg : wpsImgList) {
            result.put(wpsImg.getCellStr(), wpsImg);
        }
        XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
        Map<String, WpsImg> flotPictures = WpsImgUtil.getFlotPictures(sheet);
        result.putAll(flotPictures);
        return result;
    }

    /**
     * 获取浮动形式的图片
     * 以map方式返回
     * 键为行列格式 x-y
     *
     * @param xssfSheet
     * @return
     */
    public static Map<String, WpsImg> getFlotPictures(XSSFSheet xssfSheet) {
        Map<String, WpsImg> map = new HashMap<>();
        XSSFDrawing drawingPatriarch = xssfSheet.getDrawingPatriarch();
        if (Objects.isNull(drawingPatriarch)) {
            return map;
        }
        List<XSSFShape> list = drawingPatriarch.getShapes();
        for (XSSFShape shape : list) {
            XSSFPicture picture = (XSSFPicture) shape;
            XSSFClientAnchor xssfClientAnchor = (XSSFClientAnchor) picture.getAnchor();
            XSSFPictureData pdata = picture.getPictureData();
            // 行号-列号
            String key = xssfClientAnchor.getRow1() + "-" + xssfClientAnchor.getCol1();
            WpsImg wpsImg = new WpsImg();
            wpsImg.setPictureData(pdata);
            wpsImg.setType(1);
            map.put(key, wpsImg);
        }
        return map;
    }
}
