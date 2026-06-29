package com.example.kanpo.repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import com.example.kanpo.importer.KampoIngredientDraft;
import com.example.kanpo.importer.KampoImportDraft;
import com.example.kanpo.view.KampoProductEditForm;
import com.example.kanpo.view.KampoIngredientView;
import com.example.kanpo.view.KampoProductView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KampoImportRepository {

	private final JdbcTemplate jdbcTemplate;

	public KampoImportRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public long insertProduct(KampoImportDraft draft) {
		long id = nextId("kampo_products");
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement("""
				INSERT INTO kampo_products (
					id,
					identification_code,
					sales_name,
					reading,
					efficacy_condition_text,
					efficacy_indication_text,
					dosage_daily_amount,
					dosage_instructions_text,
					source_file_name,
					source_document_no
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""");
			ps.setLong(1, id);
			ps.setString(2, draft.getIdentificationCode());
			ps.setString(3, draft.getSalesName());
			ps.setString(4, draft.getReading());
			ps.setString(5, draft.getEfficacyConditionText());
			ps.setString(6, draft.getEfficacyIndicationText());
			ps.setBigDecimal(7, draft.getDosageDailyAmount());
			ps.setString(8, draft.getDosageInstructionsText());
			ps.setString(9, draft.getSourceFileName());
			ps.setString(10, draft.getSourceDocumentNo());
			return ps;
		});
		return id;
	}

	public void updateProduct(KampoProductEditForm form) {
		int updated = jdbcTemplate.update("""
			UPDATE kampo_products
			SET identification_code = ?,
				sales_name = ?,
				reading = ?,
				efficacy_condition_text = ?,
				efficacy_indication_text = ?,
				dosage_daily_amount = ?,
				dosage_instructions_text = ?,
				source_file_name = ?,
				source_document_no = ?,
				updated_at = CURRENT_TIMESTAMP
			WHERE id = ?
			""",
			form.getIdentificationCode(),
			form.getSalesName(),
			form.getReading(),
			form.getEfficacyConditionText(),
			form.getEfficacyIndicationText(),
			form.getDosageDailyAmount(),
			form.getDosageInstructionsText(),
			form.getSourceFileName(),
			form.getSourceDocumentNo(),
			form.getId());
		if (updated == 0) {
			throw new IllegalStateException("更新対象のデータが見つかりませんでした。");
		}
	}

	public long findOrCreateIngredient(String ingredientName) {
		List<Long> ids = jdbcTemplate.query(
				"SELECT id FROM kampo_ingredients WHERE ingredient_name = ?",
				(rs, rowNum) -> rs.getLong("id"),
				ingredientName);
		if (!ids.isEmpty()) {
			return ids.get(0);
		}

		long id = nextId("kampo_ingredients");
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement("""
				INSERT INTO kampo_ingredients (
					id,
					ingredient_name
				) VALUES (?, ?)
				""");
			ps.setLong(1, id);
			ps.setString(2, ingredientName);
			return ps;
		});
		return id;
	}

	public void insertProductIngredient(long productId, long ingredientId, KampoIngredientDraft draft) {
		long id = nextId("kampo_product_ingredients");
		jdbcTemplate.update("""
			INSERT INTO kampo_product_ingredients (
				id,
				product_id,
				ingredient_id,
				amount_value,
				amount_unit,
				sort_order,
				raw_amount_text
			) VALUES (?, ?, ?, ?, ?, ?, ?)
			""",
			id,
			productId,
			ingredientId,
			draft.getAmountValue(),
			Objects.requireNonNullElse(draft.getAmountUnit(), "g"),
			draft.getSortOrder(),
			draft.getRawAmountText());
	}

	public List<KampoProductView> findProductsByIdentificationCode(String identificationCode) {
		return findProducts("""
			SELECT
				id,
				identification_code,
				sales_name,
				reading,
				efficacy_condition_text,
				efficacy_indication_text,
				dosage_daily_amount,
				dosage_instructions_text,
				source_file_name,
				source_document_no
			FROM kampo_products
			WHERE identification_code = ?
			   OR LOWER(sales_name) LIKE LOWER(?)
			   OR LOWER(reading) LIKE LOWER(?)
			ORDER BY id DESC
			""",
			true,
			identificationCode,
			"%" + identificationCode + "%",
			"%" + identificationCode + "%");
	}

	public List<KampoProductView> findProductsByIngredientName(String ingredientName) {
		List<KampoProductView> products = findProducts("""
			SELECT DISTINCT
				p.id,
				p.identification_code,
				p.sales_name,
				p.reading,
				p.efficacy_condition_text,
				p.efficacy_indication_text,
				p.dosage_daily_amount,
				p.dosage_instructions_text,
				p.source_file_name,
				p.source_document_no
			FROM kampo_products p
			WHERE LOWER(p.sales_name) LIKE LOWER(?)
			   OR LOWER(p.reading) LIKE LOWER(?)
			   OR EXISTS (
			   	SELECT 1
			   	FROM kampo_product_ingredients pi
			   	JOIN kampo_ingredients i ON i.id = pi.ingredient_id
			   	WHERE pi.product_id = p.id
			   	  AND LOWER(i.ingredient_name) LIKE LOWER(?)
			   )
			""",
			true,
			"%" + ingredientName + "%",
			"%" + ingredientName + "%",
			"%" + ingredientName + "%");
		products.sort(productComparator());
		return products;
	}

	public List<KampoProductView> findProductsBySummaryText(String summaryText) {
		return findProducts("""
			SELECT
				id,
				identification_code,
				sales_name,
				reading,
				efficacy_condition_text,
				efficacy_indication_text,
				dosage_daily_amount,
				dosage_instructions_text,
				source_file_name,
				source_document_no
			FROM kampo_products
			WHERE LOWER(efficacy_indication_text) LIKE LOWER(?)
			   OR LOWER(sales_name) LIKE LOWER(?)
			   OR LOWER(reading) LIKE LOWER(?)
			ORDER BY id DESC
			""",
			true,
			"%" + summaryText + "%",
			"%" + summaryText + "%",
			"%" + summaryText + "%");
	}

	public List<KampoProductView> findAllProductsSortedByIdentificationCode() {
		return findAllProductsSortedByIdentificationCode(Integer.MAX_VALUE, 0);
	}

	public List<KampoProductView> findAllProductsSortedByIdentificationCode(int limit, int offset) {
		List<KampoProductView> products = findProducts("""
			SELECT
				id,
				identification_code,
				sales_name,
				reading,
				efficacy_condition_text,
				efficacy_indication_text,
				dosage_daily_amount,
				dosage_instructions_text,
				source_file_name,
				source_document_no
			FROM kampo_products
		""",
			false);
		products.sort(productComparator());
		int fromIndex = Math.min(Math.max(offset, 0), products.size());
		int safeLimit = Math.max(limit, 0);
		int toIndex = (int) Math.min((long) fromIndex + safeLimit, products.size());
		return new ArrayList<>(products.subList(fromIndex, toIndex));
	}

	public KampoProductView findProductById(long id) {
		List<KampoProductView> products = findProducts("""
			SELECT
				id,
				identification_code,
				sales_name,
				reading,
				efficacy_condition_text,
				efficacy_indication_text,
				dosage_daily_amount,
				dosage_instructions_text,
				source_file_name,
				source_document_no
			FROM kampo_products
			WHERE id = ?
		""",
			true,
			id);
		return products.isEmpty() ? null : products.get(0);
	}

	public List<KampoProductView> findProductsByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
		List<KampoProductView> products = findProducts("""
			SELECT
				id,
				identification_code,
				sales_name,
				reading,
				efficacy_condition_text,
				efficacy_indication_text,
				dosage_daily_amount,
				dosage_instructions_text,
				source_file_name,
				source_document_no
			FROM kampo_products
			WHERE id IN (""" + placeholders + """
			)
			""",
			true,
			ids.toArray());
		Map<Long, KampoProductView> productById = new LinkedHashMap<>();
		for (KampoProductView product : products) {
			productById.put(product.getId(), product);
		}
		List<KampoProductView> orderedProducts = new ArrayList<>();
		for (Long id : ids) {
			KampoProductView product = productById.get(id);
			if (product != null) {
				orderedProducts.add(product);
			}
		}
		return orderedProducts;
	}

	public long countAllProducts() {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kampo_products", Long.class);
		return count == null ? 0L : count;
	}

	private List<KampoProductView> findProducts(String sql, boolean loadIngredients, Object... args) {
		List<KampoProductView> products = jdbcTemplate.query(sql, (rs, rowNum) -> {
			KampoProductView product = new KampoProductView();
			product.setId(rs.getLong("id"));
			product.setIdentificationCode(rs.getString("identification_code"));
			product.setSalesName(rs.getString("sales_name"));
			product.setReading(rs.getString("reading"));
			product.setEfficacyConditionText(rs.getString("efficacy_condition_text"));
			product.setEfficacyIndicationText(rs.getString("efficacy_indication_text"));
			product.setDosageDailyAmount(rs.getBigDecimal("dosage_daily_amount"));
			product.setDosageInstructionsText(rs.getString("dosage_instructions_text"));
			product.setSourceFileName(rs.getString("source_file_name"));
			product.setSourceDocumentNo(rs.getString("source_document_no"));
			return product;
		}, args);

		if (loadIngredients && !products.isEmpty()) {
			loadIngredients(products);
		}

		return products;
	}

	private Comparator<KampoProductView> productComparator() {
		return Comparator
			.comparing((KampoProductView product) -> numericIdentificationCode(product.getIdentificationCode()),
				Comparator.nullsLast(Long::compareTo))
			.thenComparing(product -> Objects.requireNonNullElse(product.getIdentificationCode(), ""))
			.thenComparing(KampoProductView::getId, Comparator.reverseOrder());
	}

	private Long numericIdentificationCode(String identificationCode) {
		if (identificationCode == null || identificationCode.isBlank()) {
			return null;
		}
		for (int index = 0; index < identificationCode.length(); index++) {
			if (!Character.isDigit(identificationCode.charAt(index))) {
				return null;
			}
		}
		return Long.parseLong(identificationCode);
	}

	private long nextId(String tableName) {
		Long nextId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM " + tableName, Long.class);
		if (nextId == null) {
			throw new IllegalStateException("ID の採番に失敗しました: " + tableName);
		}
		return nextId;
	}

	private void loadIngredients(List<KampoProductView> products) {
		List<Long> productIds = products.stream().map(KampoProductView::getId).toList();
		String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
		String sql = """
			SELECT
				pi.product_id,
				pi.sort_order,
				i.ingredient_name,
				pi.amount_value,
				pi.amount_unit,
				pi.raw_amount_text
			FROM kampo_product_ingredients pi
			JOIN kampo_ingredients i ON i.id = pi.ingredient_id
			WHERE pi.product_id IN (""" + placeholders + """
			)
			ORDER BY pi.product_id, pi.sort_order, pi.id
			""";

		Map<Long, List<KampoIngredientView>> ingredientsByProductId = new LinkedHashMap<>();
		for (Long productId : productIds) {
			ingredientsByProductId.put(productId, new ArrayList<>());
		}

		jdbcTemplate.query(sql, rs -> {
			while (rs.next()) {
				Long productId = rs.getLong("product_id");
				KampoIngredientView ingredient = new KampoIngredientView();
				ingredient.setSortOrder(rs.getInt("sort_order"));
				ingredient.setIngredientName(rs.getString("ingredient_name"));
				ingredient.setAmountValue(rs.getBigDecimal("amount_value"));
				ingredient.setAmountUnit(rs.getString("amount_unit"));
				ingredient.setRawAmountText(rs.getString("raw_amount_text"));
				ingredientsByProductId.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(ingredient);
			}
			return null;
		}, productIds.toArray());

		for (KampoProductView product : products) {
			product.setIngredients(ingredientsByProductId.getOrDefault(product.getId(), new ArrayList<>()));
		}
	}
}
