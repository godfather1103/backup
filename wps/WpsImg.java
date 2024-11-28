package wps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFPictureData;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WpsImg {
    private String imgId;
    private String cellStr;
    private String rId;
    private String imgName;
    private XSSFPictureData pictureData;
    //0 嵌入单元格 1 浮动
    private int type;
}
