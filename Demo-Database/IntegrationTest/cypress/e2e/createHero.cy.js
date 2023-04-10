const testHero = 'Test Hero';
describe('Create Hero and retrieve it.', () => {
  it('passes', () => {
    cy.visit('/hero')
    cy.get('html body main.container div.row a.btn.btn-outline-secondary').click()
    cy.get('#nameInput').type(testHero)
    cy.get('#cityInput').type('Test City')
    cy.get('#universumInput').select('DC Comics')
    cy.get('.btn').click()
    cy.get('#customerTable').should('exist')
    let rows = cy.get('html body main.container div.row.col-md-7.table-responsive table#customerTable.table tbody tr')
    let row = rows.contains(testHero)
    row.should('exist')
  })
})
