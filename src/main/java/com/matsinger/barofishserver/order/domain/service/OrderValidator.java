package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.global.exception.ErrorCode;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.order.domain.model.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderValidator {
    
    public void validateOrderAccess(Orders order, TokenInfo tokenInfo) {
        if (isAdmin(tokenInfo)) {
            return;
        }
        if (isUserAccessingOwnOrder(order, tokenInfo)) {
            return;
        }
        if (isPartnerAccessingOwnProducts(order, tokenInfo)) {
            return;
        }

        throw new BusinessException(ErrorCode.NOT_ALLOWED);
    }

    private boolean isAdmin(TokenInfo tokenInfo) {
        return tokenInfo.getType().equals(TokenAuthType.ADMIN);
    }

    private boolean isUserAccessingOwnOrder(Orders order, TokenInfo tokenInfo) {
        return tokenInfo.getType().equals(TokenAuthType.USER) && 
               order.getUserId() == (tokenInfo.getId());
    }

    private boolean isPartnerAccessingOwnProducts(Orders order, TokenInfo tokenInfo) {
        if (!tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            return false;
        }
        
        return order.hasProductFromStore(tokenInfo.getId());
    }
} 