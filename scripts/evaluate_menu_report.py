"""Render separate cohorts without pooling repeated source photographs."""
def render(result):
    lines = ['# '+result.get('title','v0.5 → v0.6 메뉴 필드 비교'),'',
        '완성된 실제 앱 원시 결과를 같은 gold로 비교했다. 표본군은 합산하지 않는다. 숫자 변경의 원본 대조 결과는 아래 수동 검토 절에 별도로 기록한다.','',
        '| 표본군 | 정규화 이름 | 인쇄 단가 | 수량 | 행 금액 | 관측 필드 완전행 | 누락 | 과다 |',
        '|---|---:|---:|---:|---:|---:|---:|---:|']
    for name,c in result['cohorts'].items():
        a,b = c['before'],c['after']
        ratio = lambda v,k: str(v['fields'][k]['correct'])+'/'+str(v['fields'][k]['total'])
        fields = [ratio(a,k)+' → '+ratio(b,k) for k in a['fields']]
        t,u = a['totals'],b['totals']
        fields += [f"{t['complete_observed_rows']}/{t['rows']} → {u['complete_observed_rows']}/{u['rows']}"]
        fields += [f'{t[k]} → {u[k]}' for k in ('missing_rows','extra_rows')]
        lines.append('| '+name+' | '+' | '.join(fields)+' |')
    lines += ['','## 완전행과 퇴행','',
        '아래 완전4필드는 이름·단가·수량·행금액이 모두 인쇄된 행만 분모로 삼는다. 단가 미인쇄 행의 성공을 단가 정확도로 세지 않는다.','']
    for name,c in result['cohorts'].items():
        a,b = c['before']['totals'],c['after']['totals']; t=c['transitions']
        cer = lambda v: f"{v['cer_edits']}/{v['cer_units']}" if v['cer_units'] else '해당 없음'
        lines.append(f"- {name}: 완전4필드 {a['complete_four_field_rows']}/{a['four_field_rows']} → {b['complete_four_field_rows']}/{b['four_field_rows']}; CER {cer(a)} → {cer(b)}; 기존 정답 필드가 틀린 행 {len(t['correct_to_wrong_fields'])}, 완전행 퇴행 {len(t['complete_to_incomplete'])}.")
        for row in t['correct_to_wrong_fields']:
            lines.append(f"  - {row['file']} #{row['gold_row']}: {', '.join(row['fields'])}.")
    lines += ['','## 해석과 검토 범위',''] + ['- '+s for s in result['notes']]
    lines += ['','## 변경 숫자의 원본 대조','']
    review=result.get('number_review')
    lines += review['report_lines'] if review else [
        '수동 원본 대조 결과 작성 전이다. 이 절을 채우기 전에는 숫자 변경 검토가 완료됐다고 보고하지 않는다.']
    lines += ['',
        '전체 숫자 변경 목록과 각 행의 before/after·정답·필드 퇴행은 `'+result.get('output_prefix','MENU')+'_FINAL_FIELDS.json`에 기록했다.']
    return '\n'.join(lines)+'\n'
