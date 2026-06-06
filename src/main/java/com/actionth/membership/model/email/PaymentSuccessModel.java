// model/email/PaymentSuccessModel.java
package com.actionth.membership.model.email;
import lombok.Data;
import java.util.List;

@Data
public class PaymentSuccessModel {
  private String dateRegister;
  private List<Applicant> applicants;
  private Double totalPrice;
  private String transactionId;
  private String thaiBahtText;
  private String eventName;
  private String eventDate;
  private String refNo;
  private String eventDetailsHtml;

  @Data
  public static class Applicant {
    private String firstName;
    private String lastName;
    private String eventName;
    private String price;         // หรือ Double แล้ว format ใน template
    private String shirtSizeName;
  }
}
