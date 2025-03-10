package com.matsinger.barofishserver.domain.settlement.application;

import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementOrderDto;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementProductOptionItemDto;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementStoreDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementExcelService {

    public ByteArrayInputStream settlementExcelDownload(List<SettlementOrderDto> settlementOrderDtos) throws IOException {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("바로피쉬 정산");
            Row row = null;
            Cell cell = null;
            int rowNum = 0;

            String[] headers = {"상품번호", "주문번호", "주문상태", "주문일", "구매확정일", "파트너", "상품명", "옵션명", "과세여부", "공급가(원)", "수수료가(원)", "판매가", "배송비", "수량", "총 금액(원)", "총 주문금액", "쿠폰명", "쿠폰할인", "포인트", "최종결제금액(원)", "결제수단", "정산 비율(%)", "정산금액(원)", "정산상태", "정산일시", "수령인", "연락처", "이메일", "주소", "배송메세지", "택배사", "운송장번호"};

            // Header 설정
            row = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Body 설정
            for (int orderIdx = 0; orderIdx < settlementOrderDtos.size(); orderIdx++) {
                SettlementOrderDto settlementOrderDto = settlementOrderDtos.get(orderIdx);
                String orderId = settlementOrderDto.getOrderId();


                for (SettlementStoreDto storeDto : settlementOrderDto.getSettlementStoreDtos()) {
                    String partnerName = storeDto.getPartnerName();
                    Float settlementRate = storeDto.getSettlementRate();

                    for (SettlementProductOptionItemDto optionItemDto : storeDto.getStoreOptionItemDtos()) {
                        row = sheet.createRow(rowNum++);

                        cell = row.createCell(0);
                        cell.setCellValue(optionItemDto.getProductId());

                        cell = row.createCell(1);
                        cell.setCellValue(orderId);

                        cell = row.createCell(2);
                        cell.setCellValue(optionItemDto.getOrderProductInfoState() == OrderProductState.FINAL_CONFIRM ? "구매확정" : null);

                        cell = row.createCell(3);
                        cell.setCellValue(optionItemDto.getOrderedAt() != null ? new SimpleDateFormat("yyyy.MM.dd").format(optionItemDto.getOrderedAt()) : null);

                        cell = row.createCell(4);
                        cell.setCellValue(optionItemDto.getFinalConfirmedAt() != null ? new SimpleDateFormat("yyyy.MM.dd").format(optionItemDto.getFinalConfirmedAt()) : null);

                        cell = row.createCell(5);
                        cell.setCellValue(partnerName);

                        cell = row.createCell(6);
                        cell.setCellValue(optionItemDto.getProductName());

                        cell = row.createCell(7);
                        cell.setCellValue(optionItemDto.getOptionItemName());

                        cell = row.createCell(8);
                        cell.setCellValue(optionItemDto.isTaxFree() ? "비과세" : "과세");

                        cell = row.createCell(9);
                        cell.setCellValue(optionItemDto.getPurchasePrice());

                        cell = row.createCell(10);
                        cell.setCellValue(optionItemDto.getCommissionPrice());

                        cell = row.createCell(11);
                        cell.setCellValue(optionItemDto.getSellingPrice());

                        cell = row.createCell(12);
                        cell.setCellValue(optionItemDto.getDeliveryFee());

                        cell = row.createCell(13);
                        cell.setCellValue(optionItemDto.getQuantity());

                        cell = row.createCell(14);
                        cell.setCellValue(optionItemDto.getTotalPrice());

                        cell = row.createCell(20);
                        cell.setCellValue(OrderPaymentWay.findByOrderPaymentWay(optionItemDto.getPaymentWay()));

                        cell = row.createCell(21);
                        cell.setCellValue(settlementRate + "%");

                        cell = row.createCell(22);
                        cell.setCellValue(optionItemDto.getSettlementPrice());

                        cell = row.createCell(23);
                        cell.setCellValue(optionItemDto.isSettlementState() ? "정산완료" : "정산예정");

                        cell = row.createCell(24);
                        String settledAt = null;
                        if (optionItemDto.getSettledAt() != null) {
                            settledAt = new SimpleDateFormat("yyyy.MM.dd").format(optionItemDto.getSettledAt());
                        }
                        cell.setCellValue(settledAt);

                        cell = row.createCell(25);
                        cell.setCellValue(optionItemDto.getCustomerName());

                        cell = row.createCell(26);
                        cell.setCellValue(optionItemDto.getPhoneNumber());

                        cell = row.createCell(27);
                        cell.setCellValue(optionItemDto.getEmail());

                        cell = row.createCell(28);
                        cell.setCellValue(optionItemDto.getAddress() + " " + optionItemDto.getAddressDetail());

                        cell = row.createCell(29);
                        cell.setCellValue(optionItemDto.getDeliverMessage());

                        cell = row.createCell(30);
                        cell.setCellValue(optionItemDto.getDeliveryCompany());

                        cell = row.createCell(31);
                        cell.setCellValue(optionItemDto.getInvoiceCode());
                    }
                    row = sheet.createRow(rowNum++);

                    cell = row.createCell(0);
                    cell.setCellValue("파트너 주문정보");

                    cell = row.createCell(12);
                    cell.setCellValue(storeDto.getStoreDeliveryFeeSum());

                    cell = row.createCell(14);
                    cell.setCellValue(storeDto.getStoreTotalPriceSum());
                }
                row = sheet.createRow(rowNum++);

                cell = row.createCell(0);
                cell.setCellValue("주문정보");

                cell = row.createCell(12);
                cell.setCellValue(settlementOrderDto.getOrderDeliveryFeeSum());

                cell = row.createCell(15);
                cell.setCellValue(settlementOrderDto.getCouponName());

                cell = row.createCell(16);
                cell.setCellValue(settlementOrderDto.getCouponDiscount());

                cell = row.createCell(17);
                cell.setCellValue(settlementOrderDto.getUsePoint());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
