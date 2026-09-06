"""Minimal evaluator regressions; no Android/model dependencies."""
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from evaluate_menu_align import align, joined_cells, selected
from evaluate_menu_fields import evaluate

class MenuEvaluationTest(unittest.TestCase):
    def run_score(self, gold, actual, kind='nonnegative'):
        with TemporaryDirectory() as folder:
            p = Path(folder)
            (p/'gold.json').write_text(json.dumps({'receipts':[{'file':'a','total':10,'items':gold}]}))
            (p/'raw.json').write_text(json.dumps([{'file':'a','total':10,'items':actual}]))
            return evaluate(p/'raw.json', p/'gold.json', kind=kind)

    def test_correct_total_does_not_hide_wrong_unit_quantity(self):
        row = dict(name='가', unit=5, count=2, printedTotal=10)
        score = self.run_score([row], [{**row, 'unit':10, 'count':1}])
        self.assertEqual(score['rows'][0]['incorrect'], ['unit','count'])
        self.assertEqual(score['totals']['total_correct'], 1)
        self.assertEqual(score['totals']['complete_four_field_rows'], 0)

    def test_unknown_unit_and_free_option_amount(self):
        row = dict(name='가', unit=None, count=2, printedTotal=10)
        free = dict(name='나', unit=0, count=1, printedTotal=0, role='free_option')
        score = self.run_score([row, free], [
            {**row, 'unit':10, 'amountBased':True}, {**free, 'printedTotal':2}])
        self.assertEqual(score['fields']['unit'], {'correct':1,'total':1})
        self.assertEqual(score['roles']['free_option']['complete'], 0)
        self.assertEqual(score['rows'][1]['incorrect'], ['printedTotal'])

    def test_missing_extra_and_merged_names(self):
        a, b, c = [{'name':s} for s in ['가','나','다']]
        self.assertEqual(align([a,b],[b]), ([(0,None),(1,0)],[]))
        self.assertEqual(align([a],[c,a]), ([(0,1)],[0]))
        merged = joined_cells([a,b],[{'name':'가나'}])
        self.assertEqual(merged[0]['kind'], 'merged_names')
        split = joined_cells([{'name':'가나'}],[a,b])
        self.assertEqual(split[0]['kind'], 'split_name')

    def test_card_receipt_extra_menu_is_failure(self):
        score = self.run_score([], [dict(name='가', unit=10, count=1, printedTotal=10)])
        self.assertEqual(score['totals']['extra_rows'], 1)
        self.assertEqual(score['totals']['rows'], 0)

    def test_unknown_quantity_keeps_observed_unit_but_is_not_one(self):
        row = dict(name='가', unit=5, count=2, printedTotal=10)
        score = self.run_score([row], [{**row, 'count':0, 'quantityKnown':False, 'baseTotal':10}])
        self.assertEqual(score['rows'][0]['actual']['unit'], 5)
        self.assertIsNone(score['rows'][0]['actual']['count'])
        self.assertEqual(score['rows'][0]['incorrect'], ['count'])

    def test_removed_rows_are_not_coupons_or_included_discounts(self):
        self.assertTrue(selected(dict(count=-1, includedDiscount=False), 'removed'))
        self.assertFalse(selected(dict(count=1, unit=-10, baseTotal=-10), 'removed'))
        self.assertFalse(selected(dict(count=1, includedDiscount=True), 'removed'))

if __name__ == '__main__':
    unittest.main()
