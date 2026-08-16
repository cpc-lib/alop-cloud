# PAYMENT PROVIDER ADAPTER SPEC — V6.3

## 1. Provider SPI
Every provider implementation must map external protocol into the canonical Payment Domain contract. Provider SDK DTOs must never leak into Domain.

## 2. Canonical Mapping
| Canonical | WeChat Adapter | Alipay Adapter | UnionPay Adapter |
|---|---|---|---|
| paymentNo | merchant order number | merchant order number | merchant order number/orderId |
| channelTradeNo | provider transaction id | provider trade number | provider/query transaction id |
| amount | convert Money to provider minor unit where required | adapter validates provider amount format | convert/validate provider transaction amount |
| merchantId | configured merchant identity | configured application/merchant identity | configured merchant identity |
| appId | configured app identity when applicable | configured appId | configured app/product identity when applicable |
| callback | verify/decrypt per configured provider profile | verify signature per configured provider profile | verify signature/certificate per configured provider profile |

The exact provider SDK/API version is selected and locked during implementation; the adapter must satisfy this canonical SPEC and provider contract tests.

## 3. WeChat
Supported internal methods:
- WECHAT_MINI_PROGRAM
- WECHAT_JSAPI
- WECHAT_NATIVE
- WECHAT_H5
- WECHAT_APP

Mandatory adapter behavior:
- `paymentNo` must be sent as merchant order reference.
- amount must be generated from server Money, never client SDK.
- callback verification/decryption occurs before business processing.
- merchant/app/order/amount must all match local snapshot.
- duplicate notifications are expected and must ACK idempotently.

## 4. Alipay
Supported:
- ALIPAY_APP
- ALIPAY_WAP
- ALIPAY_PC

Mandatory:
- verify provider signature with configured public key/certificate profile;
- match merchant application identity and merchant order reference;
- exact server amount validation;
- asynchronous notification and active query both converge through the same `VerifiedPaymentResult` application path.

## 5. UnionPay
Supported:
- UNIONPAY_APP
- UNIONPAY_WEB

Mandatory:
- verify signing certificate/profile;
- map merchant order and provider query/transaction id;
- exact transaction amount validation;
- active query and callback converge through the same idempotent success command.

## 6. Common Result
All adapters return:
```text
ProviderResult {
  resultType: SUCCESS | FAILED | UNKNOWN
  providerCode
  providerMessage
  providerRequestId
  providerTradeNo?
  providerStatus?
  occurredAt?
  rawResponseHash
  clientAction?
}
```

## 7. UNKNOWN
Provider HTTP 5xx/timeout/connection reset is not automatically FAILED if the request may already have reached the provider. Adapter must classify conservatively and rely on query.

## 8. Contract Tests
One abstract provider contract test must be reused by Mock/WeChat/Alipay/UnionPay adapters to enforce SUCCESS/FAILED/UNKNOWN, signature validation, amount validation and duplicate callback behavior.
