package com.matsinger.barofishserver.domain.deliver;

import com.matsinger.barofishserver.domain.deliver.domain.Deliver;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("!local")
public class ShippingAdapterImpl implements ShippingApiAdapter {
    @Value("${smart-parcel.apiKey}")
    private String accessKey;

    String SWEET_TRACKER_BASE_URL = "http://info.sweettracker.co.kr";
    String COMPANY_LIST_URL = SWEET_TRACKER_BASE_URL + "/api/v1/companylist";
    String RECOMMEND_COMPANY_LIST_URL = SWEET_TRACKER_BASE_URL + "/api/v1/recommend";
    String TRACKING_INFO_URL = SWEET_TRACKER_BASE_URL + "/api/v1/trackingInfo";

    @Override
    public List<Deliver.Company> getRecommendDeliverCompanyList(String invoice) {
        RestTemplate restTemplate = new RestTemplate();
        String url = RECOMMEND_COMPANY_LIST_URL + "?t_key=" + accessKey + "&t_invoice=" + invoice;
        String jsonString = restTemplate.getForObject(url, String.class);
        JSONObject object = new JSONObject(jsonString);
        JSONArray arrObject = object.getJSONArray("Recommend");
        List<Deliver.Company> companies = new ArrayList<>();
        for (int i = 0; i < arrObject.length(); i++) {
            JSONObject obj = arrObject.getJSONObject(i);
            companies.add(Deliver.Company.builder().Code(obj.getString("Code")).Name(obj.getString("Name")).build());
        }
        return companies;
    }

    @Override
    public Deliver.TrackingInfo getTrackingInfo(String code, String invoice) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = TRACKING_INFO_URL + "?t_key=" + accessKey + "&t_invoice=" + invoice + "&t_code=" + code;
            String jsonString = restTemplate.getForObject(url, String.class);
            JSONObject object = new JSONObject(jsonString);
            try {
                Boolean status = object.getBoolean("status");
                return null;
            } catch (Exception e) {}
            try {
                JSONArray trackingDetailList = object.getJSONArray("trackingDetails");
                Deliver.TrackingInfo
                        trackingInfo =
                        Deliver.TrackingInfo.builder().adUrl(object.getString("adUrl")).invoiceNo(object.getString(
                                "invoiceNo")).itemImage(object.getString("itemImage")).itemName(object.getString(
                                "itemName")).level(object.getInt("level")).result(object.getString("result")).senderName(
                                object.getString("senderName")).build();
                List<Deliver.TrackingDetails> trackingDetails = new ArrayList<>();
                for (int i = 0; i < trackingDetailList.length(); i++) {
                    JSONObject obj = trackingDetailList.getJSONObject(i);
                    trackingDetails.add(Deliver.TrackingDetails.builder().code(String.valueOf(obj.get("code"))).kind(
                            String.valueOf(obj.get("kind"))).level(obj.getInt("level")).manName(obj.getString("manName")).manPic(
                            obj.getString("manPic")).timeString(obj.getString("timeString")).where(obj.getString("where")).build());
                }
                trackingInfo.setTrackingDetails(trackingDetails);
                return trackingInfo;
            } catch (Exception e) {
                return null;
//                throw new BusinessException("유효하지 않은 운송장 번호이거나 택배사 코드입니다.");
            }
        } catch (Error e) {
            return null;
        }
    }

    public String getAccessKey() {
        return accessKey;
    }
}
