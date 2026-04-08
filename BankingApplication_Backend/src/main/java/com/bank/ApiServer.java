package com.bank;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.AdminCredential;
import com.bank.model.TransactionEntry;
import com.bank.repository.AccountRepository;
import com.bank.repository.AdminRepository;
import com.bank.repository.OtpRepository;
import com.bank.repository.PasswordResetRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.AdminService;
import com.bank.service.AlertService;
import com.bank.service.AuthService;
import com.bank.service.TransactionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ApiServer {

	private static final DateTimeFormatter STATEMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter JSON_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public static void main(String[] args) {

		port(8080);
		enableCORS();
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
				.setPrettyPrinting()
				.create();

		AccountRepository accRepo = new AccountRepository();
		TransactionRepository trxRepo = new TransactionRepository();
		PasswordResetRepository passwordResetRepository = new PasswordResetRepository();
		OtpRepository otpRepository = new OtpRepository();
		AdminRepository adminRepository = new AdminRepository();

		AccountService accService = new AccountService(accRepo);
		AlertService alertService = new AlertService(new BigDecimal("1000"));
		TransactionService trxService = new TransactionService(accService, trxRepo, alertService);
		AuthService authService = new AuthService(accRepo, accService, otpRepository, passwordResetRepository);
		AdminService adminService = new AdminService(adminRepository, accRepo, accService, passwordResetRepository);

		exception(Exception.class, (error, request, response) -> {
			response.type("application/json");
			response.status(mapStatusCode(error));
			response.body(gson.toJson(messageResponse(error.getMessage())));
		});

		path("/auth", () -> {
			post("/register/request-otp", (req, res) -> {
				RegisterRequest data = gson.fromJson(req.body(), RegisterRequest.class);
				authService.sendRegistrationOtp(data.name, data.email, data.password, data.depositAmount);
				res.type("application/json");
				return gson.toJson(messageResponse("OTP sent to the provided email address."));
			});

			post("/register/verify-otp", (req, res) -> {
				OtpRequest data = gson.fromJson(req.body(), OtpRequest.class);
				authService.verifyRegistrationOtp(data.email, data.otp);
				res.type("application/json");
				return gson.toJson(messageResponse("Email verified successfully."));
			});

			post("/register", (req, res) -> {
				RegisterRequest data = gson.fromJson(req.body(), RegisterRequest.class);
				Account account = authService.register(data.name, data.email, data.password, data.depositAmount);
				res.type("application/json");
				res.status(201);
				return gson.toJson(accountResponse("Registration successful.", account));
			});

			post("/login", (req, res) -> {
				LoginRequest data = gson.fromJson(req.body(), LoginRequest.class);
				Account account = authService.login(data.email, data.password);
				res.type("application/json");
				return gson.toJson(accountResponse("Login successful.", account));
			});

			post("/forgot-password", (req, res) -> {
				EmailRequest data = gson.fromJson(req.body(), EmailRequest.class);
				authService.sendUserPasswordReset(data.email);
				res.type("application/json");
				return gson.toJson(messageResponse("Password reset link sent successfully."));
			});

			post("/reset-password", (req, res) -> {
				ResetPasswordRequest data = gson.fromJson(req.body(), ResetPasswordRequest.class);
				authService.resetPassword(data.token, data.newPassword);
				res.type("application/json");
				return gson.toJson(messageResponse("Password updated successfully."));
			});
		});

		path("/accounts", () -> {
			get("", (req, res) -> {
				Collection<Account> accounts = accService.listAllAccounts();
				res.type("application/json");
				return gson.toJson(accounts);
			});

			get("/:accNo", (req, res) -> {
				Account account = accService.getAccount(req.params("accNo"));
				res.type("application/json");
				return gson.toJson(account);
			});

			get("/:accNo/statement", (req, res) -> {
				String accNo = req.params("accNo");
				List<TransactionEntry> statement = trxService.getStatement(accNo);
				Account account = accService.getAccount(accNo);
				res.type("text/csv");
				res.header("Content-Disposition", "attachment; filename=\"statement-" + accNo + ".csv\"");
				return buildStatementCsv(account, statement);
			});
		});

		path("/transactions", () -> {
			post("/deposit", (req, res) -> {
				AmountRequest data = gson.fromJson(req.body(), AmountRequest.class);
				trxService.deposite(data.accNo, data.amount);
				Account account = accService.getAccount(data.accNo);
				res.type("application/json");
				return gson.toJson(accountResponse("Deposit successful.", account));
			});

			post("/withdraw", (req, res) -> {
				AmountRequest data = gson.fromJson(req.body(), AmountRequest.class);
				trxService.withdraw(data.accNo, data.amount);
				Account account = accService.getAccount(data.accNo);
				res.type("application/json");
				return gson.toJson(accountResponse("Withdrawal successful.", account));
			});

			post("/transfer", (req, res) -> {
				TransferRequest data = gson.fromJson(req.body(), TransferRequest.class);
				trxService.transfer(data.fromAcc, data.toAcc, data.amount);
				Map<String, Object> responseBody = new LinkedHashMap<>();
				responseBody.put("message", "Transfer successful.");
				responseBody.put("fromAccount", accService.getAccount(data.fromAcc));
				responseBody.put("toAccount", accService.getAccount(data.toAcc));
				res.type("application/json");
				return gson.toJson(responseBody);
			});
		});

		path("/admin", () -> {
			post("/login", (req, res) -> {
				LoginRequest data = gson.fromJson(req.body(), LoginRequest.class);
				AdminCredential admin = adminService.login(data.email, data.password);
				Map<String, Object> responseBody = new LinkedHashMap<>();
				responseBody.put("message", "Admin login successful.");
				responseBody.put("admin", admin);
				res.type("application/json");
				return gson.toJson(responseBody);
			});

			post("/forgot-password", (req, res) -> {
				EmailRequest data = gson.fromJson(req.body(), EmailRequest.class);
				adminService.sendPasswordReset(data.email);
				res.type("application/json");
				return gson.toJson(messageResponse("Admin reset link sent successfully."));
			});

			post("/reset-password", (req, res) -> {
				ResetPasswordRequest data = gson.fromJson(req.body(), ResetPasswordRequest.class);
				adminService.resetPassword(data.token, data.newPassword);
				res.type("application/json");
				return gson.toJson(messageResponse("Admin password updated successfully."));
			});

			get("/accounts", (req, res) -> {
				res.type("application/json");
				return gson.toJson(adminService.listAccounts());
			});

			post("/accounts/:accNo/status", (req, res) -> {
				AccountStatusRequest data = gson.fromJson(req.body(), AccountStatusRequest.class);
				Account account = adminService.updateAccountStatus(req.params("accNo"), data.status);
				res.type("application/json");
				return gson.toJson(accountResponse("Account status updated successfully.", account));
			});
		});
	}

	public static void enableCORS() {
		options("/*", (request, response) -> {
			String reqheaders = request.headers("Access-Control-Request-Headers");
			if (reqheaders != null) {
				response.header("Access-Control-Allow-Headers", reqheaders);
			}

			String reqMethod = request.headers("Access-Control-Request-Method");
			if (reqMethod != null) {
				response.header("Access-Control-Allow-Methods", reqMethod);
			}

			return "OK";

		});

		before((req, res) -> {
			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
			res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
		});

	}

	private static int mapStatusCode(Exception exception) {
		if (exception instanceof AccountNotFoundException) {
			return 404;
		}
		if (exception instanceof InvalidAmountException || exception instanceof IllegalArgumentException) {
			return 400;
		}
		if (exception instanceof InsufficientBalanceException) {
			return 409;
		}
		return 500;
	}

	private static Map<String, Object> messageResponse(String message) {
		Map<String, Object> responseBody = new LinkedHashMap<>();
		responseBody.put("message", message);
		return responseBody;
	}

	private static Map<String, Object> accountResponse(String message, Account account) {
		Map<String, Object> responseBody = messageResponse(message);
		responseBody.put("account", account);
		return responseBody;
	}

	private static String buildStatementCsv(Account account, List<TransactionEntry> statement) {
		StringBuilder builder = new StringBuilder();
		builder.append("Account Number,").append(account.getAccountNumber()).append('\n');
		builder.append("Holder Name,").append(account.getHolderName()).append('\n');
		builder.append("Email,").append(account.getEmail()).append('\n');
		builder.append("Status,").append(account.getStatus()).append('\n');
		builder.append('\n');
		builder.append("Transaction ID,Type,Amount,Target Account,Created At\n");

		for (TransactionEntry entry : statement) {
			builder.append(entry.getId()).append(',')
					.append(entry.getType()).append(',')
					.append(entry.getAmount()).append(',')
					.append(entry.getTargetAccount() == null ? "" : entry.getTargetAccount()).append(',')
					.append(entry.getCreatedAt().format(STATEMENT_DATE_FORMAT)).append('\n');
		}

		return builder.toString();
	}

	static class RegisterRequest {
		String name;
		String email;
		String password;
		BigDecimal depositAmount;
	}

	static class LoginRequest {
		String email;
		String password;
	}

	static class EmailRequest {
		String email;
	}

	static class OtpRequest {
		String email;
		String otp;
	}

	static class ResetPasswordRequest {
		String token;
		String newPassword;
	}

	static class AmountRequest {
		String accNo;
		BigDecimal amount;
	}

	static class TransferRequest {
		String fromAcc;
		String toAcc;
		BigDecimal amount;
	}

	static class AccountStatusRequest {
		String status;
	}

	static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

		@Override
		public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
			return src == null ? null : new JsonPrimitive(src.format(JSON_DATE_FORMAT));
		}

		@Override
		public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			return json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), JSON_DATE_FORMAT);
		}
	}
}
