package com.matsinger.barofishserver.excel.application;

import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.OptionItemState;
import com.matsinger.barofishserver.domain.product.domain.OptionState;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductState;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import lombok.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ProductExcelService {
    @Value("${cloud.aws.s3.imageUrl}")
    private String s3Url;
    private final Common utils;
    private final ExcelService excelService;
    private final StoreService storeService;
    private final CategoryQueryService categoryQueryService;
    private final ProductService productService;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductExcelData {
        Product product;
        Option option;
        List<OptionItem> optionItems;
        Integer representOptionNo;
    }

    public ProductExcelData convertRow2ProductData(Row row) {
        // 0 입점사 ID
        String partnerId = row.getCell(0).getStringCellValue();
        Optional<Store> store = storeService.selectOptionalStoreByLoginId(partnerId);
        if (store == null || store.isEmpty()) throw new BusinessException(String.format("%s 는 없는 입점사 ID 입니다.", partnerId));
        // 3 상품명
        String productName = row.getCell(3).getStringCellValue();
        if (productName == null) throw new BusinessException("상품명이 비었습니다. 확인해주세요.");
        Optional<Product>
                product =
                productService.findOptionalProductWithTitleAndStoreId(productName, store.get().getId());
        // 1 1차 카테고리
        String parentCategoryName = row.getCell(1) != null ? row.getCell(1).getStringCellValue() : null;
        if (parentCategoryName == null) throw new BusinessException("1차 카테고리가 비었습니다. 확인해주세요.");
        Optional<Category> parentCategory = categoryQueryService.findOptionalCategoryWithName(parentCategoryName);
        if (parentCategory == null || parentCategory.isEmpty())
            throw new BusinessException(String.format("%s는 존재하지 않는 1차 카테고리입니다.", parentCategoryName));
        // 2 2차 카테고리
        String categoryName = row.getCell(2) != null ? row.getCell(2).getStringCellValue() : null;
        if (categoryName == null) throw new BusinessException("2차 카테고리가 비었습니다. 확인해주세요.");
        Optional<Category>
                category =
                categoryQueryService.findOptionalCategoryWithName(categoryName, parentCategory.get().getId());
        if (category == null || category.isEmpty())
            throw new BusinessException(String.format("%s는 존재하지 않는 2차 카테고리입니다.", categoryName));
        // 4 도착 예정일
        int expectedDeliverDay = row.getCell(4) != null ? (int) (row.getCell(4).getNumericCellValue()) : 0;
        // 5 발송안내
        String deliveryInfo = row.getCell(5) != null ? row.getCell(5).getStringCellValue() : "";
        // 6 배송비    `
        Cell deliveryFeeCell = row.getCell(6);
        if (deliveryFeeCell == null) throw new BusinessException(String.format("'%s' 의 배송비를 확인해주세요. ", productName));
        int deliveryFee = (int) deliveryFeeCell.getNumericCellValue();
        // 7 택배 책정  수량
        Cell deliverBoxPerAmountCell = row.getCell(7);
        if (deliverBoxPerAmountCell == null)
            throw new BusinessException(String.format("'%s' 의 택배 책정 수량을 확인해주세요. ", productName));
        Integer deliverBoxPerAmount = (int) deliverBoxPerAmountCell.getNumericCellValue();
        // 8 노출 상태 (노출 | 미노출)
        String stateStr = row.getCell(8) != null ? row.getCell(8).getStringCellValue() : "미노출";
        ProductState state = stateStr.equals("노출") ? ProductState.ACTIVE : ProductState.INACTIVE;
        // 9 과세여부 (과세 | 비과세)
        String needTaxationStr = row.getCell(9) != null ? row.getCell(9).getStringCellValue() : "비과세";
        Boolean needTaxation = needTaxationStr.equals("과세");
        // 10 옵션 여부
        // 13 옵션명
        Cell optionNameCell = row.getCell(13);
        if (optionNameCell == null) throw new BusinessException(String.format("'%s' 의 옵션명을 확인해주세요. ", productName));
        List<String> optionNameList = Arrays.stream(row.getCell(13).getStringCellValue().split("\n")).toList();
        // 11 매입가
        Cell purchasePriceCell = row.getCell(11);
        if (purchasePriceCell == null) throw new BusinessException(String.format("'%s' 의 매입가를 확인해주세요. ", productName));

        List<Integer>
                purchasePriceList = purchasePriceCell.getCellType().equals(CellType.NUMERIC) ?
                List.of((int) purchasePriceCell.getNumericCellValue()):
                Arrays.stream(purchasePriceCell.getStringCellValue().split("\n")).map(Integer::parseInt).toList();
        // 12 대표 옵션 번호
        Integer representOptionNo = row.getCell(12) != null ? (int) row.getCell(12).getNumericCellValue() : 0;
        // 14 옵션 정가
        Cell originPriceCell = row.getCell(14);
        if (originPriceCell == null) throw new BusinessException(String.format("'%s' 의 옵션 정가를 확인해주세요. ", productName));
        List<Integer>
                originPriceList =
                originPriceCell.getCellType().equals(CellType.NUMERIC) ?
                        List.of((int) originPriceCell.getNumericCellValue()):
                Arrays.stream(originPriceCell.getStringCellValue().split("\n")).map(Integer::parseInt).toList();
        // 15 옵션 할인가
        Cell discountPriceCell = row.getCell(15);
        if (discountPriceCell == null) throw new BusinessException(String.format("'%s' 의 옵션 할인가 확인해주세요. ", productName));
        List<Integer>
                discountPriceList =
                discountPriceCell.getCellType().equals(CellType.NUMERIC) ?
                        List.of((int) discountPriceCell.getNumericCellValue()):
                Arrays.stream(discountPriceCell.getStringCellValue().split("\n")).map(Integer::parseInt).toList();
        // 16 최대 주문 수량
        Cell maxAvailableAmountCell = row.getCell(16);
        if (maxAvailableAmountCell == null) throw new BusinessException(String.format("'%s' 의 최대 주문 수량을 확인해주세요.", productName));
        List<Integer>
                maxAvailableAmountList =
                maxAvailableAmountCell.getCellType().equals(CellType.NUMERIC) ?
                        List.of((int) maxAvailableAmountCell.getNumericCellValue()):
                Arrays.stream(maxAvailableAmountCell.getStringCellValue().split("\n")).map(Integer::parseInt).toList();
        // 17 재고
        Cell amountCell = row.getCell(17);
        if (amountCell == null) throw new BusinessException(String.format("'%s' 의 재고를 확인해주세요.", productName));
        List<Integer>
                amountList =
                amountCell.getCellType().equals(CellType.NUMERIC) ?
                        List.of((int) amountCell.getNumericCellValue()):
                Arrays.stream(amountCell.getStringCellValue().split("\n")).map(Integer::parseInt).toList();
        // 18 적립금 지급
        Float pointRate = row.getCell(18) != null ? (float) row.getCell(18).getNumericCellValue() : null;
        ProductExcelData result = new ProductExcelData();
        Product updatedProduct = null;
        List<OptionItem> updatedOptionItemList = new ArrayList<>();
        if (new HashSet<>(List.of(purchasePriceList.size(),
                originPriceList.size(),
                discountPriceList.size(),
                originPriceList.size(),
                maxAvailableAmountList.size(),
                amountList.size())).size() != 1) throw new BusinessException("옵션 정보의 개수를 일치시켜 주세요.");
        if (product.isPresent()) {
            product.get().setCategory(category.get());
            product.get().setExpectedDeliverDay(expectedDeliverDay);
            product.get().setDeliveryInfo(deliveryInfo);
//            product.get().setDeliveryFee(deliveryFee);
            product.get().setDeliverBoxPerAmount(deliverBoxPerAmount);
            product.get().setState(product.get().getState().equals(ProductState.DELETED) ? ProductState.DELETED : state);
            product.get().setNeedTaxation(needTaxation);
            Option option = productService.selectProductNeededOption(product.get().getId());

            for (int i = 0; i < originPriceList.size(); i++) {
                Optional<OptionItem>
                        optionItem =
                        productService.selectProductWithName(optionNameList.get(i), option.getId());
                if (optionItem.isPresent()) {
                    optionItem.get().setPurchasePrice(purchasePriceList.get(i));
                    optionItem.get().setOriginPrice(originPriceList.get(i));
                    optionItem.get().setDiscountPrice(discountPriceList.get(i));
                    optionItem.get().setMaxAvailableAmount(maxAvailableAmountList.get(i));
                    optionItem.get().setAmount(amountList.get(i));
                    updatedOptionItemList.add(optionItem.get());
                } else {
                    OptionItem
                            newOptionItem =
                            OptionItem.builder().option(option).name(optionNameList.get(i)).state(
                                    OptionItemState.ACTIVE).discountPrice(discountPriceList.get(i)).amount(amountList.get(
                                    i)).purchasePrice(purchasePriceList.get(i)).originPrice(originPriceList.get(i)).deliverFee(
                                    0).maxAvailableAmount(maxAvailableAmountList.get(i)).build();
                    updatedOptionItemList.add(newOptionItem);
                }
            }
            result.setProduct(product.get());
            result.setOption(option);
            result.setOptionItems(updatedOptionItemList);
        } else {
            Product
                    newProduct =
                    Product.builder().storeId(store.get().getId()).category(category.get()).state(state).images("[" +
                            s3Url +
                            "/default_product.png]").title(productName).originPrice(0).discountRate(0).deliveryInfo(
                            deliveryInfo).descriptionImages(s3Url + "/default_description.html").expectedDeliverDay(
                            expectedDeliverDay).createdAt(utils.now()).needTaxation(needTaxation).pointRate(pointRate).deliverBoxPerAmount(
                            deliverBoxPerAmount).build();
            Option
                    option =
                    Option.builder().state(OptionState.ACTIVE).isNeeded(true).description("").build();
            for (int i = 0; i < optionNameList.size(); i++) {
                OptionItem
                        optionItem =
                        OptionItem.builder().name(optionNameList.get(i)).state(OptionItemState.ACTIVE).discountPrice(
                                discountPriceList.get(i)).amount(amountList.get(i)).purchasePrice(purchasePriceList.get(
                                i)).originPrice(originPriceList.get(i)).deliverFee(0).maxAvailableAmount(
                                maxAvailableAmountList.get(i)).build();
                updatedOptionItemList.add(optionItem);
            }
            result.setProduct(newProduct);
            result.setOption(option);
            result.setOptionItems(updatedOptionItemList);
        }
        result.setRepresentOptionNo(representOptionNo);
        return result;
    }

    public void processProductExcelDataList(List<ProductExcelData> dataList) {
        for (ProductExcelData data : dataList) {
            Product product = productService.upsertProduct(data.product);
            if (data.option.getProduct().getId() == 0) data.option.setProduct(product);
            Option option = productService.upsertOption(data.option);
            data.optionItems.forEach(v -> {
                v.setOption(option);
            });
            List<OptionItem> optionItems = productService.upsertOptionItemList(data.optionItems);
            product.setRepresentOptionItemId(optionItems.get(data.representOptionNo - 1).getId());
            productService.upsertProduct(product);
        }
    }


    public void processProductExcel(MultipartFile file) {
        XSSFSheet sheet = excelService.readExcel(file);
        Iterator<Row> rowIterator = sheet.iterator();
        Row firstRow = rowIterator.next();
        List<String>
                columnName =
                List.of("입점사ID",
                        "1차 카테고리",
                        "2차 카테고리",
                        "상품명",
                        "도착 예정일",
                        "발송안내",
                        "배송비",
                        "택배 책정 수량",
                        "노출상태",
                        "과세여부",
                        "옵션여부",
                        "매입가",
                        "대표옵션 번호",
                        "옵션명",
                        "옵션 정가",
                        "옵션 할인가",
                        "최대 주문 수량",
                        "재고",
                        "적립금지급");
        excelService.checkUploadExcelValid(firstRow, columnName);
        List<ProductExcelData> productList = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row.getPhysicalNumberOfCells() != columnName.size()) break;
            ProductExcelData data = convertRow2ProductData(row);
            productList.add(data);
        }
        processProductExcelDataList(productList);
    }
}
