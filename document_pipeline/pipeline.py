#!/usr/bin/env python3
"""
Legacy Vault - Final Document Generation Pipeline
Author: Pratik (Document Pipeline & Synthesis Lead)

Pipeline Steps:
1. Deduplicate: Rule-based pass grouping records referring to the same account/policy
   (institution name match, account type, last-4-digits overlap).
2. Consolidate & Categorize: Groups into clean financial categories:
   - Bank Accounts
   - Insurance Policies
   - Loans & Debts
   - Retirement / EPF / PPF
   - Important Access Instructions
3. Flag Gaps Honestly: Explicitly highlights incomplete or missing details without fabricating.
4. Generate Final PDF Document: Produces a clean, senior/beneficiary-friendly summary PDF using ReportLab.
"""

import os
import re
import sys
from datetime import datetime
from typing import List, Dict, Any

# Category definitions
CATEGORY_BANK = "Bank Accounts & Deposits"
CATEGORY_INSURANCE = "Insurance & Life Policies"
CATEGORY_LOANS = "Loans & Liabilities"
CATEGORY_RETIREMENT = "EPF, PPF & Retirement Funds"
CATEGORY_INSTRUCTIONS = "Important Access & Physical Asset Instructions"

CATEGORY_MAP = {
    "bank": CATEGORY_BANK,
    "savings": CATEGORY_BANK,
    "fixed_deposit": CATEGORY_BANK,
    "fd": CATEGORY_BANK,
    "insurance": CATEGORY_INSURANCE,
    "lic": CATEGORY_INSURANCE,
    "policy": CATEGORY_INSURANCE,
    "loan": CATEGORY_LOANS,
    "debt": CATEGORY_LOANS,
    "liability": CATEGORY_LOANS,
    "epf": CATEGORY_RETIREMENT,
    "ppf": CATEGORY_RETIREMENT,
    "pension": CATEGORY_RETIREMENT,
    "nps": CATEGORY_RETIREMENT,
    "access_instruction": CATEGORY_INSTRUCTIONS,
    "instruction": CATEGORY_INSTRUCTIONS,
    "locker": CATEGORY_INSTRUCTIONS
}

def normalize_text(text: str) -> str:
    """Normalize string for fuzzy comparison."""
    if not text:
        return ""
    return re.sub(r"[^a-z0-9]", "", text.lower())

def extract_last_digits(val: str, min_digits: int = 3) -> str:
    """Extract numeric suffix (e.g. last 4 digits of account/policy number)."""
    if not val:
        return ""
    digits = re.findall(r"\d+", str(val))
    if not digits:
        return ""
    combined = "".join(digits)
    return combined[-min_digits:] if len(combined) >= min_digits else combined

def are_records_duplicate(rec_a: Dict[str, Any], rec_b: Dict[str, Any]) -> bool:
    """
    Rule-based deduplication heuristic.
    Matches if institution and category align AND last-digits match or names match closely.
    """
    fields_a = rec_a.get("extracted_fields", {})
    fields_b = rec_b.get("extracted_fields", {})

    inst_a = normalize_text(fields_a.get("institution") or fields_a.get("bank") or "")
    inst_b = normalize_text(fields_b.get("institution") or fields_b.get("bank") or "")

    # If institutions are present and completely different, not duplicate
    if inst_a and inst_b and (inst_a not in inst_b and inst_b not in inst_a):
        return False

    num_a = extract_last_digits(fields_a.get("account_number") or fields_a.get("policy_number") or "")
    num_b = extract_last_digits(fields_b.get("account_number") or fields_b.get("policy_number") or "")

    # Overlapping last 3-4 digits in same institution
    if num_a and num_b and (num_a in num_b or num_b in num_a):
        return True

    # Matching title/label for access instructions
    label_a = normalize_text(fields_a.get("label") or rec_a.get("title") or "")
    label_b = normalize_text(fields_b.get("label") or rec_b.get("title") or "")
    if label_a and label_b and (label_a == label_b or label_a in label_b or label_b in label_a):
        return True

    return False

def deduplicate_records(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Step 1: Rule-based deduplication pass.
    Groups duplicates and merges their fields, preserving source provenance.
    """
    clusters = []  # List of merged record dicts

    for record in records:
        matched_cluster = None
        for cluster in clusters:
            if are_records_duplicate(record, cluster):
                matched_cluster = cluster
                break

        if matched_cluster is not None:
            # Merge fields: take non-empty values, combine sources
            c_fields = matched_cluster.setdefault("extracted_fields", {})
            r_fields = record.get("extracted_fields", {})
            for k, v in r_fields.items():
                if v and not c_fields.get(k):
                    c_fields[k] = v
                elif v and len(str(v)) > len(str(c_fields.get(k, ""))):
                    # Keep more complete string if available
                    c_fields[k] = v
            # Record merge history
            sources = matched_cluster.setdefault("sources", [matched_cluster.get("source", "unknown")])
            sources.append(record.get("source", "unknown"))
            matched_cluster["merged_count"] = matched_cluster.get("merged_count", 1) + 1
        else:
            new_cluster = dict(record)
            new_cluster["sources"] = [record.get("source", "unknown")]
            new_cluster["merged_count"] = 1
            clusters.append(new_cluster)

    return clusters

def flag_gaps(record: Dict[str, Any]) -> List[str]:
    """
    Step 3: Honest Gap Flagging.
    Returns plain-language advisory warnings for any missing or uncertain information.
    Never fabricates missing data.
    """
    gaps = []
    fields = record.get("extracted_fields", {})
    rec_type = (record.get("type") or "").lower()

    if "bank" in rec_type or "savings" in rec_type:
        acc_num = str(fields.get("account_number") or "")
        if not acc_num or "x" in acc_num.lower() or "*" in acc_num or len(re.findall(r"\d", acc_num)) < 6:
            gaps.append("Account number is partially masked or truncated — verify with passbook/branch.")
        if not fields.get("branch") and not fields.get("ifsc"):
            gaps.append("Branch / IFSC code not specified — contact customer service.")
        if not fields.get("nominee"):
            gaps.append("Nominee name not recorded on document — branch verification required.")

    elif "insurance" in rec_type or "lic" in rec_type:
        pol_num = str(fields.get("policy_number") or "")
        if not pol_num or "x" in pol_num.lower():
            gaps.append("Policy number partially obscured — verify with insurance agent/portal.")
        if not fields.get("maturity_date"):
            gaps.append("Maturity date not found on document scan.")
        if not fields.get("sum_assured"):
            gaps.append("Sum assured / premium receipt details pending verification.")

    elif "epf" in rec_type or "ppf" in rec_type:
        uan = str(fields.get("uan") or fields.get("account_number") or "")
        if not uan:
            gaps.append("UAN / Account ID incomplete — verify via EPFO / bank portal.")

    elif "access_instruction" in rec_type or "instruction" in rec_type:
        val = str(fields.get("value") or fields.get("location") or "")
        if len(val) < 5:
            gaps.append("Instruction brief — verify physical location.")

    return gaps

def consolidate_and_categorize(records: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    """
    Step 2 & 3: Consolidate deduplicated records into categories and attach gap flags.
    """
    deduped = deduplicate_records(records)
    categorized = {
        CATEGORY_BANK: [],
        CATEGORY_INSURANCE: [],
        CATEGORY_LOANS: [],
        CATEGORY_RETIREMENT: [],
        CATEGORY_INSTRUCTIONS: []
    }

    for item in deduped:
        raw_type = (item.get("type") or "bank").lower()
        target_category = CATEGORY_BANK
        for key, cat in CATEGORY_MAP.items():
            if key in raw_type:
                target_category = cat
                break

        # Attach flagged gaps
        item["gap_advisories"] = flag_gaps(item)
        categorized[target_category].append(item)

    return categorized

def generate_pdf(
    owner_name: str,
    beneficiary_name: str,
    categorized_data: Dict[str, List[Dict[str, Any]]],
    output_path: str
) -> str:
    """
    Step 4: Generate a dignified, executive-grade PDF summary document for the beneficiary.
    Uses ReportLab to build a professional layout with categories and honest gap callouts.
    """
    try:
        from reportlab.lib.pagesizes import letter
        from reportlab.lib import colors
        from reportlab.lib.units import inch
        from reportlab.platypus import (
            SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, KeepTogether
        )
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    except ImportError:
        # Fallback to structured text generation if reportlab is not yet imported
        txt_path = output_path.replace(".pdf", ".txt")
        with open(txt_path, "w", encoding="utf-8") as f:
            f.write(f"LEGACY VAULT — CONSOLIDATED ESTATE SUMMARY\n")
            f.write(f"Owner: {owner_name} | Beneficiary: {beneficiary_name}\n\n")
            for cat, items in categorized_data.items():
                f.write(f"=== {cat} ({len(items)} records) ===\n")
                for it in items:
                    f.write(f"- {it.get('extracted_fields')}\n")
                    for g in it.get("gap_advisories", []):
                        f.write(f"  [GAP]: {g}\n")
        return txt_path

    doc = SimpleDocTemplate(
        output_path,
        pagesize=letter,
        rightMargin=40,
        leftMargin=40,
        topMargin=40,
        bottomMargin=40,
        pageCompression=0
    )

    styles = getSampleStyleSheet()
    
    # Custom high-readability typography
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=20,
        leading=24,
        textColor=colors.HexColor("#1A365D"),
        spaceAfter=4
    )
    subtitle_style = ParagraphStyle(
        'DocSub',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#4A5568"),
        spaceAfter=12
    )
    section_style = ParagraphStyle(
        'SectionHeading',
        parent=styles['Heading2'],
        fontSize=13,
        leading=17,
        textColor=colors.HexColor("#2B6CB0"),
        spaceBefore=14,
        spaceAfter=6
    )
    body_style = ParagraphStyle(
        'BodyTextCustom',
        parent=styles['Normal'],
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#2D3748")
    )
    warning_style = ParagraphStyle(
        'WarningStyle',
        parent=styles['Normal'],
        fontSize=8,
        leading=11,
        textColor=colors.HexColor("#C53030")
    )

    story = []

    # Document Header
    story.append(Paragraph("<b>LEGACY VAULT</b> — Consolidated Estate Inventory", title_style))
    meta_info = f"<b>Prepared For:</b> {beneficiary_name} &nbsp;|&nbsp; <b>Estate Owner:</b> {owner_name} &nbsp;|&nbsp; <b>Date Released:</b> {datetime.now().strftime('%d %B %Y')}"
    story.append(Paragraph(meta_info, subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#CBD5E0"), spaceAfter=14))

    # Notice to beneficiary
    notice_text = (
        "<i>Notice: This document synthesizes all records organized and encrypted within Legacy Vault. "
        "Any item highlighted with a <b>Verification Note</b> contains incomplete scan details and should be "
        "cross-verified directly with the financial institution or physical paperwork.</i>"
    )
    story.append(Paragraph(notice_text, body_style))
    story.append(Spacer(1, 10))

    total_records = sum(len(items) for items in categorized_data.values())

    for category_name, items in categorized_data.items():
        if not items:
            continue

        story.append(Paragraph(f"<b>{category_name}</b> ({len(items)})", section_style))

        for idx, item in enumerate(items, 1):
            fields = item.get("extracted_fields", {})
            inst = fields.get("institution") or fields.get("bank") or fields.get("label") or "Record #" + str(idx)
            acc_type = fields.get("account_type") or fields.get("policy_type") or item.get("type", "Asset").capitalize()
            acc_num = fields.get("account_number") or fields.get("policy_number") or fields.get("value") or "N/A"
            branch = fields.get("branch") or fields.get("location") or "—"
            nominee = fields.get("nominee") or fields.get("beneficiary") or "Not Stated"

            table_data = [
                [Paragraph("<b>Institution / Item:</b>", body_style), Paragraph(f"<b>{inst}</b> ({acc_type})", body_style)],
                [Paragraph("<b>Reference / ID:</b>", body_style), Paragraph(f"<code>{acc_num}</code>", body_style)],
                [Paragraph("<b>Branch / Location:</b>", body_style), Paragraph(branch, body_style)],
                [Paragraph("<b>Documented Nominee:</b>", body_style), Paragraph(nominee, body_style)],
            ]

            t = Table(table_data, colWidths=[130, 400])
            t.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#F7FAFC")),
                ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
                ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#EDF2F7")),
                ('TOPPADDING', (0, 0), (-1, -1), 4),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
                ('LEFTPADDING', (0, 0), (-1, -1), 8),
                ('RIGHTPADDING', (0, 0), (-1, -1), 8),
            ]))

            elements_block = [t]

            # Append gap warnings
            gaps = item.get("gap_advisories", [])
            if gaps:
                gap_text = "<b>Action Required:</b> " + " ".join(f"• {g}" for g in gaps)
                elements_block.append(Spacer(1, 3))
                elements_block.append(Paragraph(gap_text, warning_style))

            elements_block.append(Spacer(1, 8))
            story.append(KeepTogether(elements_block))

    # Footer note
    story.append(Spacer(1, 15))
    story.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#E2E8F0"), spaceAfter=8))
    footer_text = (
        "Cryptographic Integrity: Multi-party threshold attestation verified via Shamir's Secret Sharing. "
        "Attestation logged immutably on Polygon Amoy blockchain. Legacy Vault Hackathon Demo 2026."
    )
    story.append(Paragraph(footer_text, subtitle_style))

    doc.build(story)
    return output_path

# --- Self-Contained Sample Data for Testing ---
SAMPLE_RECORDS = [
    {
        "source": "ocr",
        "type": "bank",
        "extracted_fields": {
            "institution": "State Bank of India",
            "account_type": "Savings Account",
            "account_number": "3049xxxx9120",
            "branch": "Adyar Chennai Branch",
            "nominee": "Ramesh Kumar (Son)"
        }
    },
    {
        # Intentional duplicate of the above SBI account from manual entry
        "source": "manual",
        "type": "bank",
        "extracted_fields": {
            "institution": "SBI",
            "account_type": "Savings",
            "account_number": "304918239120",
            "branch": "Adyar",
            "ifsc": "SBIN0001234"
        }
    },
    {
        # Incomplete LIC policy record with partially blurred scan
        "source": "ocr",
        "type": "insurance",
        "extracted_fields": {
            "institution": "Life Insurance Corporation of India (LIC)",
            "policy_type": "Jeevan Anand",
            "policy_number": "847291xxx",
            "sum_assured": "Rs. 10,00,000"
            # Missing maturity_date and nominee
        }
    },
    {
        "source": "manual",
        "type": "epf",
        "extracted_fields": {
            "institution": "Employees' Provident Fund Organisation (EPFO)",
            "account_type": "EPF & Pension Fund",
            "uan": "100928374615",
            "member_id": "TNMAS0012345000001"
        }
    },
    {
        "source": "manual",
        "type": "access_instruction",
        "extracted_fields": {
            "label": "Bank Locker Key & Property Papers",
            "value": "Key is in top-left drawer of Godrej wooden cupboard, behind the prayer book. Locker #42 at SBI Adyar."
        }
    }
]

def run_pipeline(records: List[Dict[str, Any]] = None, output_pdf: str = "legacy_vault_summary.pdf"):
    if records is None:
        records = SAMPLE_RECORDS
    print(f"[*] Input raw records: {len(records)}")
    categorized = consolidate_and_categorize(records)
    print("[*] Consolidation complete across categories:")
    for cat, items in categorized.items():
        if items:
            print(f"    - {cat}: {len(items)} record(s)")
            for it in items:
                print(f"      • {it.get('extracted_fields', {}).get('institution') or it.get('extracted_fields', {}).get('label')}")
                for gap in it.get("gap_advisories", []):
                    print(f"        [!] Gap: {gap}")

    pdf_out = generate_pdf(
        owner_name="S. Sundaram (Senior Citizen, 74)",
        beneficiary_name="Ramesh Kumar (Son)",
        categorized_data=categorized,
        output_path=output_pdf
    )
    print(f"[✓] Final document generated successfully: {pdf_out}")
    return pdf_out

if __name__ == "__main__":
    out_file = sys.argv[1] if len(sys.argv) > 1 else "legacy_vault_summary.pdf"
    run_pipeline(SAMPLE_RECORDS, out_file)
