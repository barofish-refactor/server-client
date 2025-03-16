package com.matsinger.barofishserver.excel.application;

import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.domain.StoreDeliverFeeType;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.domain.StoreState;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.S3.Uploader;
import lombok.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartnerExcelService {
    private final Uploader uploader;
    private final StoreService storeService;
    private final Common utils;
    private final ExcelService excelService;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class StoreData {
        Store store;
        StoreInfo storeInfo;
    }

    public StoreData convertRow2StoreInfo(Row row) {
        //입점사명 0
        String partnerName = row.getCell(0).getStringCellValue();
        // 입점사 ID 1
        String loginId = row.getCell(1).getStringCellValue();
        Optional<Store> store = storeService.selectOptionalStoreByLoginId(loginId);
        // 비밀번호 2
        String password = row.getCell(2).getStringCellValue();
        // 판매상태 ( 정상 | 정지 ) 3
        StoreState state = row.getCell(3).getStringCellValue().equals("정상") ? StoreState.ACTIVE : StoreState.BANNED;
        // 위치 4
        String location = row.getCell(6) == null ? "" : row.getCell(4).getStringCellValue();
        // 키워드 5
        String keyword = row.getCell(5).getStringCellValue();
        // 배송비 6
        Integer deliveryFee = row.getCell(6) == null ? 0 : (int) row.getCell(6).getNumericCellValue();
        // 배송비 유형 (무료 | 유료 ) 7
        StoreDeliverFeeType
                deliverFeeType =
                row.getCell(7).getStringCellValue().equals("무료") ? StoreDeliverFeeType.FREE : row.getCell(8) ==
                        null ? StoreDeliverFeeType.FIX : StoreDeliverFeeType.FREE_IF_OVER;
        // 무료 배송 최소 금액 8
        Cell minOrderPriceCell = row.getCell(8);
        Integer minOrderPrice = minOrderPriceCell == null ? null : (int) minOrderPriceCell.getNumericCellValue();
        // 정산 비율 9
        String settlementRateStr = row.getCell(9).getStringCellValue();
        if (settlementRateStr.equals("공급가")) settlementRateStr = "100";
        settlementRateStr = settlementRateStr.replaceAll("[^\\d]", "");
        Float settlementRate = Float.valueOf(settlementRateStr);
        // 은행명 10
        String bankName = row.getCell(10) == null ? null : row.getCell(10).getStringCellValue();
        // 예금주 11
        String bankHolder = row.getCell(11) == null ? null : row.getCell(11).getStringCellValue();
        // 계좌번호 12
        String bankAccount = row.getCell(12) == null ? null : row.getCell(12).getStringCellValue();
        // 대표자 이름 13
        String representativeName = row.getCell(13) == null ? null : row.getCell(13).getStringCellValue();
        // 사업자번호 14
        String companyId = row.getCell(14) == null ? null : row.getCell(14).getStringCellValue();
        // 업태/종목 15
        String businessType = row.getCell(15) == null ? null : row.getCell(15).getStringCellValue();
        // 통신판매신고번호 16
        String mosRegistrationNumber = row.getCell(16) == null ? null : row.getCell(16).getStringCellValue();
        // 사업장 주소 17
        String businessAddress = row.getCell(17) == null ? null : row.getCell(17).getStringCellValue();
        // 우편번호 18
        String postalCode = row.getCell(18) == null ? null : row.getCell(18).getStringCellValue();
        // 지번 19
        String lotNumberAddress = row.getCell(19) == null ? null : row.getCell(19).getStringCellValue();
        // 도로명 20
        String streetNameAddress = row.getCell(120) == null ? null : row.getCell(20).getStringCellValue();
        // 상세주소 21
        String addressDetail = row.getCell(21) == null ? null : row.getCell(21).getStringCellValue();
        // 전화번호 22
        String tel = row.getCell(22) == null ? null : row.getCell(22).getStringCellValue();
        // 이메일 23
        String email = row.getCell(23) == null ? null : row.getCell(23).getStringCellValue();
        // 팩스번호 24
        String faxNumber = row.getCell(24) == null ? null : row.getCell(24).getStringCellValue();

        if (store.isPresent()) {
            StoreInfo storeInfo = storeService.selectStoreInfo(store.get().getId());
            store.get().setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
            store.get().setState(state);
            storeInfo.setName(partnerName);
            storeInfo.setLocation(location);
            storeInfo.setKeyword(keyword);
            storeInfo.setSettlementRate(settlementRate);
            storeInfo.setBankName(bankName);
            storeInfo.setBankHolder(bankHolder);
            storeInfo.setBankAccount(bankAccount);
            storeInfo.setRepresentativeName(representativeName);
            storeInfo.setCompanyId(companyId);
            storeInfo.setBusinessType(businessType);
            storeInfo.setMosRegistrationNumber(mosRegistrationNumber);
            storeInfo.setBusinessAddress(businessAddress);
            storeInfo.setPostalCode(postalCode);
            storeInfo.setLotNumberAddress(lotNumberAddress);
            storeInfo.setStreetNameAddress(streetNameAddress);
            storeInfo.setAddressDetail(addressDetail);
            storeInfo.setTel(tel);
            storeInfo.setEmail(email);
            storeInfo.setFaxNumber(faxNumber);
            return StoreData.builder().store(store.get()).storeInfo(storeInfo).build();
        } else {
            Store
                    newStore =
                    Store.builder().state(state).loginId(loginId).password(BCrypt.hashpw(password,
                            BCrypt.gensalt())).joinAt(utils.now()).build();
            StoreInfo
                    storeInfo =
                    StoreInfo.builder()
                            .backgroundImage(uploader.getS3Url() + "/default_backgroud.png")
                            .profileImage(uploader.getS3Url() +
                            "/default_profile.png").name(partnerName).location(location).keyword(keyword).visitNote("").oneLineDescription(
                            "").settlementRate(settlementRate).bankName(bankName).bankHolder(bankHolder).bankAccount(
                            bankAccount).representativeName(representativeName).companyId(companyId).businessType(
                            businessType).mosRegistrationNumber(mosRegistrationNumber).businessAddress(businessAddress).postalCode(
                            postalCode).lotNumberAddress(lotNumberAddress).streetNameAddress(streetNameAddress).addressDetail(
                            addressDetail).tel(tel).email(email).faxNumber(faxNumber).build();
            return StoreData.builder().store(newStore).storeInfo(storeInfo).build();
        }
    }

    public void processPartnerExcel(MultipartFile file) {
        XSSFSheet sheet = excelService.readExcel(file);
        Iterator<Row> rowIterator = sheet.iterator();
        Row firstRow = rowIterator.next();
        List<String>
                columnName =
                List.of("입점사명",
                        "입점사ID",
                        "비밀번호",
                        "판매상태",
                        "위치",
                        "키워드",
                        "배송비",
                        "배송비 유형",
                        "무료 배송 최소 금액",
                        "정산비율",
                        "은행명",
                        "예금주",
                        "계좌번호",
                        "대표자이름",
                        "사업자번호",
                        "업태/종목",
                        "통신판매신고번호",
                        "사업장주소",
                        "우편번호",
                        "지번",
                        "도로명",
                        "상세주소",
                        "전화번호",
                        "이메일",
                        "팩스번호");
        excelService.checkUploadExcelValid(firstRow, columnName);
        List<StoreData> storeDataList = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row.getPhysicalNumberOfCells() == 1) break;
            StoreData storeData = convertRow2StoreInfo(row);
            storeDataList.add(storeData);
        }

        for (StoreData data : storeDataList) {
            Store store = storeService.updateStore(data.store);
            data.getStoreInfo().setStoreId(store.getId());
            StoreInfo storeInfo = storeService.updateStoreInfo(data.getStoreInfo());
        }
    }
}
