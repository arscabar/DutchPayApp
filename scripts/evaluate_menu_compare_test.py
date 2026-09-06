"""Small comparison regressions; no production or OCR execution."""
import unittest
from evaluate_menu_compare import transitions, financial_changes
from evaluate_menu_report import render

class ComparisonTest(unittest.TestCase):
    def test_field_regression_despite_other_correction(self):
        base={'file':'a','gold_row':1,'expected':{'name':'가','unit':None,'count':2,'printedTotal':10},'actual':{}}
        a={'rows':[{**base,'incorrect':['name']}]}
        b={'rows':[{**base,'incorrect':['count']}]}
        r=transitions(a,b)
        self.assertEqual(r['correct_to_wrong_fields'][0]['fields'],['count'])
        self.assertEqual(r['wrong_to_correct_fields'][0]['fields'],['name'])
        self.assertEqual(r['complete_to_incomplete'],[])

    def test_unit_quantity_change_with_equal_line_total(self):
        item={'name':'가','unit':5,'count':2,'printedTotal':10}
        a=[{'file':'a','total':10,'items':[item]}]
        b=[{'file':'a','total':10,'items':[{**item,'unit':10,'count':1}]}]
        r=financial_changes(a,b)
        self.assertEqual(len(r),1)
        self.assertEqual(r[0]['fields'],['unit','count'])

    def test_added_and_removed_rows_remain_visible(self):
        a=[{'file':'a','items':[{'name':'가','unit':10,'count':1}]}]
        b=[{'file':'a','items':[]}]
        self.assertIsNone(financial_changes(a,b)[0]['after'])
        self.assertIsNone(financial_changes(b,a)[0]['before'])

if __name__=='__main__':
    unittest.main()
